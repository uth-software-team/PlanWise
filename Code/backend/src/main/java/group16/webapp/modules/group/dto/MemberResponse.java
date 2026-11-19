package group16.webapp.modules.group.dto;

import group16.webapp.modules.group.api.GroupRole;
import group16.webapp.modules.group.api.MembershipStatus;

import java.time.Instant;

/** A group member as seen by other ACTIVE members: identity and role only, never personal tasks or calendars. */
public record MemberResponse(
        Long userId,
        String fullName,
        String email,
        GroupRole role,
        MembershipStatus status,
        Instant joinedAt
) {
}
