package group16.webapp.modules.availability.api;

import java.time.Instant;
import java.util.List;

/**
 * Public contract of the availability module for scheduling (CG-06). Ranges are half-open and computed in the
 * user's timezone (users.timezone, DST-safe). Implemented in AV-04 (rules) and AV-03 (fixed events).
 */
public interface AvailabilityApi {

    /** Whether the user has at least one active availability rule (otherwise 400 NO_AVAILABILITY_RULE on generate). */
    boolean hasActiveRule(Long userId);

    /** Active availability rules expanded into ranges inside {@code [start, end)}. */
    List<TimeRange> availableIntervals(Long userId, Instant start, Instant end);

    /** ONCE and WEEKLY fixed-event occurrences overlapping {@code [start, end)}. */
    List<TimeRange> fixedEventOccurrences(Long userId, Instant start, Instant end);
}
