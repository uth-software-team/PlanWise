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
        requireActiveLeader(groupId, callerId);
        group.update(request.groupName().trim(), normalizeDescription(request.description()));
        return toResponse(groupRepository.saveAndFlush(group));
    }

    // ---------- GR-03 membership mutations (serialized per group by the TeamGroup row lock) ----------

    @Override
    @Transactional
    public MemberResponse addMember(Long callerId, Long groupId, MemberAddRequest request) {
        requireActiveCaller(callerId);
        lockGroup(groupId);
        requireActiveLeader(groupId, callerId);
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
        TeamGroup group = lockGroup(groupId);
        requireActiveLeader(groupId, callerId);
        GroupMember target = findActiveMember(groupId, userId);
        GroupRole newRole = request.role();
        if (target.getRole() != newRole) {
            if (newRole == GroupRole.MEMBER) {
                // Demoting a LEADER: demoting a peer is creator-only; self-demotion needs another leader.
                if (!userId.equals(callerId)) {
                    requireCreator(group, callerId);
                }
                requireAnotherActiveLeader(groupId);
            }
            target.changeRole(newRole);
            memberRepository.saveAndFlush(target);
        }
        return toMemberResponse(target, userApi.findById(userId).orElse(null));
    }

    @Override
    @Transactional
    public void removeMember(Long callerId, Long groupId, Long userId) {
        requireActiveCaller(callerId);
        TeamGroup group = lockGroup(groupId);
        GroupMember target;
        if (userId.equals(callerId)) {
            target = requireActiveMembership(groupId, callerId);
        } else {
            requireActiveLeader(groupId, callerId);
            target = findActiveMember(groupId, userId);
            if (target.getRole() == GroupRole.LEADER) {
                requireCreator(group, callerId);
            }
        }
        if (target.getRole() == GroupRole.LEADER) {
            requireAnotherActiveLeader(groupId);
        }
        target.deactivate(now());
        memberRepository.saveAndFlush(target);
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
        GroupMember member = requireActiveMembership(groupId, userId);
        if (member.getRole() != GroupRole.LEADER) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        return member;
    }

    private GroupMember findActiveMember(Long groupId, Long userId) {
        return memberRepository.findByGroupIdAndUserIdAndStatus(groupId, userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    /** Creator-only authority = ACTIVE LEADER (checked by the caller) and the group's creator. */
    private static void requireCreator(TeamGroup group, Long callerId) {
        if (!group.getCreatedByUserId().equals(callerId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    /** Called when an ACTIVE LEADER is about to stop being one: at least 2 ACTIVE LEADERs must exist now. */
    private void requireAnotherActiveLeader(Long groupId) {
        if (memberRepository.countByGroupIdAndRoleAndStatus(groupId, GroupRole.LEADER, MembershipStatus.ACTIVE) < 2) {
            throw new AppException(ErrorCode.DATA_CONFLICT);
        }
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

    /** Loads other members' details; call only after the caller's access has been checked. */
    private GroupResponse toResponse(TeamGroup group) {
        List<GroupMember> members = memberRepository
                .findByGroupIdAndStatusOrderByJoinedAtAscIdAsc(group.getId(), MembershipStatus.ACTIVE);
        Map<Long, UserSummary> users = members.stream()
                .flatMap(m -> userApi.findById(m.getUserId()).stream())
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));
        List<MemberResponse> memberResponses = members.stream()
                .map(m -> toMemberResponse(m, users.get(m.getUserId())))
                .toList();
        return new GroupResponse(group.getId(), group.getGroupName(), group.getDescription(),
                group.getCreatedByUserId(), memberResponses);
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
