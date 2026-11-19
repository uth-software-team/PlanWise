package group16.webapp.modules.availability.api;

import java.time.Instant;

/** Half-open time range {@code [start, end)}; adjacent ranges do not overlap. */
public record TimeRange(Instant start, Instant end) {
}
