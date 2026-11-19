package group16.webapp.modules.schedule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * Generate request (design doc SchedulingRequestDTO, CG-07). There is no user id: the user always comes from the
 * authenticated caller. The window is half-open {@code [windowStart, windowEnd)}; missing or malformed values are
 * 400 INVALID_INPUT, and {@code windowStart >= windowEnd} is 400 INVALID_TIME_RANGE. {@code includeTaskIds}
 * optionally limits the batch to those of the caller's schedulable tasks; omitted = all of them.
 */
public record GenerateScheduleRequest(
        @NotNull Instant windowStart,
        @NotNull Instant windowEnd,
        List<@NotNull Long> includeTaskIds
) {
}
