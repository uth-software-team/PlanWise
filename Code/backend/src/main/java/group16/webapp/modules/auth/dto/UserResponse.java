package group16.webapp.modules.auth.dto;

import java.time.Instant;
import java.time.LocalTime;

/** Profile of the current user returned by the API (never contains the password hash). */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String timezone,
        LocalTime planningStart,
        LocalTime planningEnd,
        Instant createdAt
) {
}
