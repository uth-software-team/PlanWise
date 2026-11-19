package group16.webapp.modules.availability.api;

import java.time.Instant;
import java.util.List;

/** Busy time of one user for other modules (CG-06; GR-04 group availability). Implemented in CAL-01. */
public interface CalendarApi {

    /**
     * Fixed-event occurrences plus CONFIRMED/COMPLETED sessions overlapping {@code [start, end)}, as time ranges
     * only: never ids, titles or notes (privacy, CG-11).
     */
    List<TimeRange> busyIntervals(Long userId, Instant start, Instant end);
}
