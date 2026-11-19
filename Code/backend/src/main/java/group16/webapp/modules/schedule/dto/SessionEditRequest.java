package group16.webapp.modules.schedule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/**
 * Moves one PROPOSED session (design doc SessionEditRequest, BR-10). {@code version} is the version of the batch
 * that owns the session, not the session version. Missing or malformed values are 400 INVALID_INPUT, and
 * {@code startAt >= endAt} is 400 INVALID_TIME_RANGE.
 */
public record SessionEditRequest(
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        @NotNull @PositiveOrZero Integer version
) {
}
