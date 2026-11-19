package group16.webapp.modules.schedule.api;

import java.time.Instant;

/**
 * A committed session for other modules (CG-06): half-open {@code [startAt, endAt)}. No task title; callers that
 * need one ask TaskApi, and privacy-sensitive callers (GR-04) use only the time range.
 */
public record ScheduledSession(
        Long sessionId,
        Long taskId,
        Instant startAt,
        Instant endAt,
        SessionStatus status
) {
}
