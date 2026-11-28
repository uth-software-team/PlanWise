package group16.webapp.modules.group.service.impl;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.group.api.GroupAccessApi;
import group16.webapp.modules.group.api.GroupRole;
import group16.webapp.modules.group.api.MembershipStatus;
import group16.webapp.modules.group.dto.GroupRequest;
import group16.webapp.modules.group.dto.GroupResponse;
import group16.webapp.modules.group.dto.GroupSummaryResponse;
import group16.webapp.modules.group.dto.LeaderTransferRequest;
import group16.webapp.modules.group.dto.MemberAddRequest;
import group16.webapp.modules.group.dto.MemberResponse;
import group16.webapp.modules.group.dto.RoleUpdateRequest;
import group16.webapp.modules.group.entity.GroupMember;
import group16.webapp.modules.group.entity.TeamGroup;
import group16.webapp.modules.group.repository.GroupMemberRepository;
import group16.webapp.modules.group.repository.TeamGroupRepository;
import group16.webapp.modules.group.service.GroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService, GroupAccessApi {

    private final TeamGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserApi userApi;

    public GroupServiceImpl(TeamGroupRepository groupRepository, GroupMemberRepository memberRepository,
                            UserApi userApi) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userApi = userApi;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> listMyGroups(Long callerId) {
        requireActiveCaller(callerId);
        Map<Long, GroupRole> roleByGroup = memberRepository.findByUserIdAndStatus(callerId, MembershipStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(GroupMember::getGroupId, GroupMember::getRole));
        return groupRepository.findAllById(roleByGroup.keySet()).stream()
                .sorted(Comparator.comparing(TeamGroup::getId))
                .map(g -> new GroupSummaryResponse(g.getId(), g.getGroupName(), roleByGroup.get(g.getId())))
                .toList();
    }

    @Override
    @Transactional
    public GroupResponse createGroup(Long callerId, GroupRequest request) {
        requireActiveCaller(callerId);
        TeamGroup group = groupRepository.save(
                new TeamGroup(callerId, request.groupName().trim(), normalizeDescription(request.description())));
        memberRepository.save(new GroupMember(group.getId(), callerId, GroupRole.LEADER, Instant.now()));
        return toResponse(group);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroup(Long callerId, Long groupId) {
        requireActiveCaller(callerId);
        TeamGroup group = requireGroup(groupId);
        requireActiveMembership(groupId, callerId);
        return toResponse(group);
    }

    @Override
    @Transactional
    public GroupResponse updateGroup(Long callerId, Long groupId, GroupRequest request) {
        requireActiveCaller(callerId);
        // Lock first, so a concurrent demotion or removal of the caller is committed before the role is checked.
        TeamGroup group = lockGroup(groupId);
        requireAllowed(groupId, callerId, GroupRole::canUpdateGroup);
        group.update(request.groupName().trim(), normalizeDescription(request.description()));
        return toResponse(groupRepository.saveAndFlush(group));
    }

    // ---------- GR-03 membership mutations (serialized per group by the TeamGroup row lock) ----------

    @Override
    @Transactional
    public MemberResponse addMember(Long callerId, Long groupId, MemberAddRequest request) {
        requireActiveCaller(callerId);
        lockGroup(groupId);
        requireAllowed(groupId, callerId, GroupRole::canAddMembers);
        UserSummary target = userApi.findByEmail(request.email())
                .filter(UserSummary::active)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        Instant now = now();
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, target.id()).orElse(null);
        if (member == null) {
            member = new GroupMember(groupId, target.id(), GroupRole.MEMBER, now);
        } else if (member.getStatus() == MembershipStatus.ACTIVE) {
            throw new AppException(ErrorCode.DATA_CONFLICT);
        } else {
            member.reactivate(now);
        }
        return toMemberResponse(memberRepository.saveAndFlush(member), target);
    }

    @Override
    @Transactional
    public MemberResponse changeMemberRole(Long callerId, Long groupId, Long userId, RoleUpdateRequest request) {
        requireActiveCaller(callerId);
        lockGroup(groupId);
        requireAllowed(groupId, callerId, GroupRole::canChangeMemberRoles);
        GroupMember target = findActiveMember(groupId, userId);
        if (target.getRole() != request.role()) {
            // D16: leadership changes only through transferLeadership or the LEADER leaving with a successor.
            throw new AppException(ErrorCode.DATA_CONFLICT);
        }
        return toMemberResponse(target, userApi.findById(userId).orElse(null));
    }

    @Override
    @Transactional
    public GroupResponse transferLeadership(Long callerId, Long groupId, LeaderTransferRequest request) {
        requireActiveCaller(callerId);
        TeamGroup group = lockGroup(groupId);
        GroupMember caller = requireAllowed(groupId, callerId, GroupRole::canTransferLeadership);
        Long newLeaderId = request.newLeaderUserId();
        if (newLeaderId.equals(callerId)) {
            throw new AppException(ErrorCode.DATA_CONFLICT);
        }
        GroupMember newLeader = findActiveMember(groupId, newLeaderId);
        userApi.findById(newLeaderId)
                .filter(UserSummary::active)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        handOver(caller, newLeader, false);
        return toResponse(group);
    }

    @Override
    @Transactional
    public void removeMember(Long callerId, Long groupId, Long userId, Long expectedSuccessorUserId) {
        boolean selfLeave = userId.equals(callerId);
        if (!selfLeave && expectedSuccessorUserId != null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        requireActiveCaller(callerId);
        lockGroup(groupId);
        if (!selfLeave) {
            requireAllowed(groupId, callerId, GroupRole::canRemoveMembers);
            GroupMember target = findActiveMember(groupId, userId);
            if (target.getRole() == GroupRole.LEADER) {
                // Unreachable while D16 holds (the caller is the only LEADER); kept as a guard.
                throw new AppException(ErrorCode.DATA_CONFLICT);
            }
            target.deactivate(now());
            memberRepository.saveAndFlush(target);
            return;
        }
        GroupMember caller = requireActiveMembership(groupId, callerId);
        if (caller.getRole() != GroupRole.LEADER) {
            if (expectedSuccessorUserId != null) {
                // The client still believes the caller is the LEADER.
                throw new AppException(ErrorCode.STALE_VERSION);
            }
            caller.deactivate(now());
            memberRepository.saveAndFlush(caller);
            return;
        }
        ActiveMembers active = loadActiveMembers(groupId);
        Long successorId = pickSuccessor(active.members(), active.users(), callerId);
        if (successorId == null) {
            throw new AppException(ErrorCode.DATA_CONFLICT);
        }
        if (!successorId.equals(expectedSuccessorUserId)) {
            throw new AppException(ErrorCode.STALE_VERSION);
        }
        GroupMember successor = active.members().stream()
                .filter(m -> m.getUserId().equals(successorId))
                .findFirst()
                .orElseThrow();
        handOver(caller, successor, true);
    }

    /**
     * The old LEADER is demoted (or deactivated when leaving) and flushed before the new one is promoted, because
     * the V11 partial unique index allows one ACTIVE LEADER per group and is not deferrable.
     */
    private void handOver(GroupMember oldLeader, GroupMember newLeader, boolean oldLeaderLeaves) {
        if (oldLeaderLeaves) {
            oldLeader.deactivate(now());
        } else {
            oldLeader.changeRole(GroupRole.MEMBER);
        }
        memberRepository.saveAndFlush(oldLeader);
        newLeader.changeRole(GroupRole.LEADER);
        memberRepository.saveAndFlush(newLeader);
    }

    // ---------- GroupAccessApi ----------

    @Override
    @Transactional(readOnly = true)
    public void requireMember(Long groupId, Long userId) {
        requireGroup(groupId);
        requireActiveMembership(groupId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireLeader(Long groupId, Long userId) {
        requireGroup(groupId);
        requireActiveLeader(groupId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveMember(Long groupId, Long userId) {
        return groupRepository.existsById(groupId)
                && memberRepository.findByGroupIdAndUserIdAndStatus(groupId, userId, MembershipStatus.ACTIVE)
                        .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> memberIds(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            return List.of();
        }
        return memberRepository.findByGroupIdAndStatusOrderByJoinedAtAscIdAsc(groupId, MembershipStatus.ACTIVE)
                .stream().map(GroupMember::getUserId).toList();
    }

    /** SELECT ... FOR UPDATE on the group; must run before any membership of the group is read. */
    private TeamGroup lockGroup(Long groupId) {
        return groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private GroupMember requireActiveLeader(Long groupId, Long userId) {
        return requireAllowed(groupId, userId, role -> role == GroupRole.LEADER);
    }

    /** 403 FORBIDDEN unless the user is an ACTIVE member whose role grants the permission. */
    private GroupMember requireAllowed(Long groupId, Long userId, Predicate<GroupRole> permission) {
        GroupMember member = requireActiveMembership(groupId, userId);
        if (!permission.test(member.getRole())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        return member;
    }

    private GroupMember findActiveMember(Long groupId, Long userId) {
        return memberRepository.findByGroupIdAndUserIdAndStatus(groupId, userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** A token stays valid for 24h, so a deleted or deactivated account must be rejected here. */
    private void requireActiveCaller(Long callerId) {
        userApi.findById(callerId)
                .filter(UserSummary::active)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
    }

    private TeamGroup requireGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private GroupMember requireActiveMembership(Long groupId, Long callerId) {
        return memberRepository.findByGroupIdAndUserIdAndStatus(groupId, callerId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN));
    }

    /** ACTIVE members in contract order (joinedAt, then id) with their accounts; missing accounts are absent. */
    private record ActiveMembers(List<GroupMember> members, Map<Long, UserSummary> users) {}

    /** Loads other members' details; call only after the caller's access has been checked. */
    private ActiveMembers loadActiveMembers(Long groupId) {
        List<GroupMember> members = memberRepository
                .findByGroupIdAndStatusOrderByJoinedAtAscIdAsc(groupId, MembershipStatus.ACTIVE);
        Map<Long, UserSummary> users = members.stream()
                .flatMap(m -> userApi.findById(m.getUserId()).stream())
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));
        return new ActiveMembers(members, users);
    }

    /**
     * The member who becomes LEADER when {@code leaderUserId} leaves: the first other member, in the given order,
     * whose account exists and is active; {@code null} when there is none. The single rule for both the DELETE check
     * and {@link GroupResponse#successorUserId()} (D16, O1, O10).
     */
    static Long pickSuccessor(List<GroupMember> orderedActiveMembers, Map<Long, UserSummary> usersById,
                              Long leaderUserId) {
        for (GroupMember member : orderedActiveMembers) {
            Long userId = member.getUserId();
            UserSummary user = usersById.get(userId);
            if (!userId.equals(leaderUserId) && user != null && user.active()) {
                return userId;
            }
        }
        return null;
    }

    /** Includes other members' details; call only after the caller's access has been checked. */
    private GroupResponse toResponse(TeamGroup group) {
        ActiveMembers active = loadActiveMembers(group.getId());
        Long leaderId = active.members().stream()
                .filter(m -> m.getRole() == GroupRole.LEADER)
                .map(GroupMember::getUserId)
                .findFirst()
                .orElse(null);
        Long successorId = leaderId == null ? null : pickSuccessor(active.members(), active.users(), leaderId);
        List<MemberResponse> memberResponses = active.members().stream()
                .map(m -> toMemberResponse(m, active.users().get(m.getUserId())))
                .toList();
        return new GroupResponse(group.getId(), group.getGroupName(), group.getDescription(),
                group.getCreatedByUserId(), memberResponses, successorId);
    }

    private static MemberResponse toMemberResponse(GroupMember member, UserSummary user) {
        return new MemberResponse(member.getUserId(),
                user == null ? null : user.fullName(),
                user == null ? null : user.email(),
                member.getRole(), member.getStatus(), member.getJoinedAt());
    }

    /** The column is NOT NULL; a missing description is stored as an empty string (GroupRequest contract). */
    private static String normalizeDescription(String description) {
        return description == null ? "" : description;
    }
}
