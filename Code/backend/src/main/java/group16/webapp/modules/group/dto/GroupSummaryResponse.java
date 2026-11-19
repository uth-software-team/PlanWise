package group16.webapp.modules.group.dto;

import group16.webapp.modules.group.api.GroupRole;

/** One entry of GET /v1/groups: a group the caller is an ACTIVE member of. */
public record GroupSummaryResponse(
        Long id,
        String groupName,
        GroupRole myRole
) {
}
