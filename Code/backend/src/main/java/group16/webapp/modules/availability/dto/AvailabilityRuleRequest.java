package group16.webapp.modules.availability.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Weekly availability rule (design doc AvailabilityRuleDTO; E3). dayOfWeek is ISO (1 = Monday ... 7 = Sunday);
 * times are wall-clock in the user's timezone, startTime &lt; endTime within one day (24:00 is not a valid time).
 * {@code active} is optional and defaults to true.
 */
public record AvailabilityRuleRequest(
        @NotNull @Min(1) @Max(7) Integer dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Boolean active
) {
}
