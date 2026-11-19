package group16.webapp.modules.availability.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import group16.webapp.modules.availability.api.RecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Fixed event (design doc FixedEventDTO; E2). ONCE requires startAt and endAt (may cross midnight). WEEKLY
 * requires dayOfWeek (ISO 1-7), startTime and endTime (one day). effectiveFrom/effectiveUntil are optional dates.
 * Ordering (start &lt; end) and fields of the other recurrence type are checked by the service (AV-03).
 */
public record FixedEventRequest(
        @NotBlank @Size(max = 255) String title,
        String note,
        @NotNull RecurrenceType recurrenceType,
        @Min(1) @Max(7) Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Instant startAt,
        Instant endAt,
        LocalDate effectiveFrom,
        LocalDate effectiveUntil
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "ONCE needs startAt and endAt; WEEKLY needs dayOfWeek, startTime and endTime")
    public boolean isRequiredRecurrenceFieldsPresent() {
        if (recurrenceType == null) {
            return true; // reported by @NotNull
        }
        return switch (recurrenceType) {
            case ONCE -> startAt != null && endAt != null;
            case WEEKLY -> dayOfWeek != null && startTime != null && endTime != null;
        };
    }
}
