package group16.webapp.modules.group.service.impl;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.group.api.GroupRole;
import group16.webapp.modules.group.api.MembershipStatus;
import group16.webapp.modules.group.dto.GroupRequest;
import group16.webapp.modules.group.dto.GroupResponse;
import group16.webapp.modules.group.dto.GroupSummaryResponse;
import group16.webapp.modules.group.dto.MemberResponse;
import group16.webapp.modules.group.entity.GroupMember;
import group16.webapp.modules.group.entity.TeamGroup;
import group16.webapp.modules.group.repository.GroupMemberRepository;
import group16.webapp.modules.group.repository.TeamGroupRepository;
import group16.webapp.modules.group.service.GroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {

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
        TeamGroup group = requireGroup(groupId);
        if (requireActiveMembership(groupId, callerId).getRole() != GroupRole.LEADER) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        group.update(request.groupName().trim(), normalizeDescription(request.description()));
        return toResponse(groupRepository.saveAndFlush(group));
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
