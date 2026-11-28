package group16.webapp.modules.group.api;

import java.util.List;

/**
 * Public contract of the group module for other modules (assignment, GR-04 group availability).
 * Roles are checked per group (team decision D8). Implemented in GR-03.
 */
public interface GroupAccessApi {

    /**
     * Requires {@code userId} to be an ACTIVE member of {@code groupId}.
     *
     * @throws group16.webapp.common.exception.AppException RESOURCE_NOT_FOUND if the group does not exist,
     *                                                      FORBIDDEN if the user is not an ACTIVE member
     */
    void requireMember(Long groupId, Long userId);

    /**
     * Requires {@code userId} to be an ACTIVE LEADER of {@code groupId}.
     *
     * @throws group16.webapp.common.exception.AppException RESOURCE_NOT_FOUND if the group does not exist,
     *                                                      FORBIDDEN if the user is not an ACTIVE LEADER
     */
    void requireLeader(Long groupId, Long userId);

    /** Non-throwing check (e.g. validating an assignee); {@code false} for a missing group. */
    boolean isActiveMember(Long groupId, Long userId);

    /** User ids of the ACTIVE members, leaders included; empty for a missing group. */
    List<Long> memberIds(Long groupId);
}
