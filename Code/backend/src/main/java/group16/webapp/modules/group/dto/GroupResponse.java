package group16.webapp.modules.group.dto;

import java.util.List;

/** Group details for ACTIVE members (design doc GroupDTO). */
public record GroupResponse(
        Long id,
        String groupName,
        String description,
        Long createdByUserId,
        List<MemberResponse> members
) {
}
