package group16.webapp.modules.group.dto;

import java.util.List;

/**
 * Group details for ACTIVE members (design doc GroupDTO). {@code members} are ordered by joinedAt, then membership
 * id (contract). {@code successorUserId} is the member who becomes LEADER if the current LEADER leaves: the first
 * other ACTIVE member with an active account in that order, or {@code null} when there is none (D16).
 */
public record GroupResponse(
        Long id,
        String groupName,
        String description,
        Long createdByUserId,
        List<MemberResponse> members,
        Long successorUserId
) {
}
