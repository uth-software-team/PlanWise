package group16.webapp.modules.availability.dto;

import group16.webapp.modules.availability.api.RecurrenceType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Fixed event as returned by the API. {@code warnings} may contain SESSION_AFFECTED when a create/update overlaps
 * CONFIRMED/COMPLETED sessions (HC-09: a warning, not an error); empty until the check exists.
 */
public record FixedEventResponse(
        Long id,
        String title,
        String note,
        RecurrenceType recurrenceType,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Instant startAt,
        Instant endAt,
        LocalDate effectiveFrom,
        LocalDate effectiveUntil,
        @ArraySchema(schema = @Schema(allowableValues = {"SESSION_AFFECTED"})) List<String> warnings,
        Instant createdAt,
        Instant updatedAt
) {
}
