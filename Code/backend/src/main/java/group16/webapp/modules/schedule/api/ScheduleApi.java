package group16.webapp.modules.schedule.api;

import java.time.Instant;
import java.util.List;

/** Public contract of the schedule module for other modules (CG-06; used by CAL-01 and GR-04). Implemented in SC-07. */
public interface ScheduleApi {

    /**
     * The user's CONFIRMED and COMPLETED sessions overlapping {@code [start, end)} (half-open: a session ending
     * exactly at {@code start} is excluded). PROPOSED and CANCELLED sessions are never returned.
     */
    List<ScheduledSession> committedSessions(Long userId, Instant start, Instant end);
}
