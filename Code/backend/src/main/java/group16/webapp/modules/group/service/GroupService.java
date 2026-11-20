package group16.webapp.modules.group.service;

import group16.webapp.modules.group.dto.GroupRequest;
import group16.webapp.modules.group.dto.GroupResponse;
import group16.webapp.modules.group.dto.GroupSummaryResponse;

import java.util.List;

/**
 * Group CRUD (GR-02). Every method takes the authenticated caller's id and answers 401 UNAUTHENTICATED when that
 * user no longer exists or is inactive, 404 RESOURCE_NOT_FOUND for a missing group and 403 FORBIDDEN when the
 * caller lacks the required ACTIVE membership or role.
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
}
