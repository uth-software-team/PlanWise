package group16.webapp.modules.group.service;

import group16.webapp.modules.group.dto.GroupRequest;
import group16.webapp.modules.group.dto.GroupResponse;
import group16.webapp.modules.group.dto.GroupSummaryResponse;
import group16.webapp.modules.group.dto.LeaderTransferRequest;
import group16.webapp.modules.group.dto.MemberAddRequest;
import group16.webapp.modules.group.dto.MemberResponse;
import group16.webapp.modules.group.dto.RoleUpdateRequest;

import java.util.List;

/**
 * Groups and members (GR-02, GR-03). Every method takes the authenticated caller's id and answers 401
 * UNAUTHENTICATED when that user no longer exists or is inactive, 404 RESOURCE_NOT_FOUND for a missing group and
 * 403 FORBIDDEN when the caller lacks the required ACTIVE membership or role. A group with ACTIVE members has exactly
 * one ACTIVE LEADER (D16); leadership changes only by {@link #transferLeadership} or by the LEADER leaving with a
 * successor.
 */
public interface GroupService {

    /** Groups the caller is an ACTIVE member of, with the caller's role in each. */
    List<GroupSummaryResponse> listMyGroups(Long callerId);

    /** Creates the group and the caller's ACTIVE LEADER membership in one transaction. */
    GroupResponse createGroup(Long callerId, GroupRequest request);

    /** ACTIVE members only. */
    GroupResponse getGroup(Long callerId, Long groupId);

    /** ACTIVE LEADER only; replaces name and description. */
    GroupResponse updateGroup(Long callerId, Long groupId, GroupRequest request);

    /** ACTIVE LEADER only; adds the user with this email as MEMBER, or reactivates the former membership as MEMBER. */
    MemberResponse addMember(Long callerId, Long groupId, MemberAddRequest request);

    /**
     * ACTIVE LEADER only. Roles cannot change here: requesting the target's current role is a no-op (200), any
     * other role is 409 DATA_CONFLICT. 404 when the target is not an ACTIVE member.
     */
    MemberResponse changeMemberRole(Long callerId, Long groupId, Long userId, RoleUpdateRequest request);

    /**
     * ACTIVE LEADER only. The caller becomes MEMBER and the target LEADER, in one transaction. 409 DATA_CONFLICT
     * when the target is the caller; 404 when the target is not an ACTIVE member or their account is inactive.
     */
    GroupResponse transferLeadership(Long callerId, Long groupId, LeaderTransferRequest request);

    /**
     * Removal by the ACTIVE LEADER (userId other than the caller; {@code expectedSuccessorUserId} must be null,
     * else 400 INVALID_INPUT; the LEADER cannot be removed, 409 DATA_CONFLICT) or self-leave (userId = caller).
     * A MEMBER leaves without {@code expectedSuccessorUserId}. The LEADER must send the successor shown in
     * {@link GroupResponse#successorUserId()}; 409 DATA_CONFLICT when there is none. A role or successor that no
     * longer matches is 409 STALE_VERSION and changes nothing.
     */
    void removeMember(Long callerId, Long groupId, Long userId, Long expectedSuccessorUserId);
}
