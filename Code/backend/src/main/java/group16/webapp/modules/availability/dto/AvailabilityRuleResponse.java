package group16.webapp.modules.availability.dto;

import java.time.Instant;
import java.time.LocalTime;

/** Availability rule as returned by the API. */
public record AvailabilityRuleResponse(
        Long id,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean active,
        Instant createdAt
) {
}
