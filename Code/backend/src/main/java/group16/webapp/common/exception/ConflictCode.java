package group16.webapp.common.exception;

/**
 * Per-session scheduling violation (design doc enum "ConflictCode"), returned inside the
 * {@code conflicts[]} payload of {@link ErrorCode#CONSTRAINT_VIOLATION} (HTTP 409).
 * Request-level problems such as an invalid time range use {@link ErrorCode} instead.
 */
public enum ConflictCode {
    /** HC-01: overlaps another session of the same user. */
    SESSION_OVERLAP,
    /** HC-02: overlaps an occurrence of a fixed event. */
    FIXED_EVENT_OVERLAP,
    /** HC-03: not fully inside an active availability rule. */
    OUTSIDE_AVAILABILITY,
    /** HC-06: ends after the task deadline. */
    DEADLINE_EXCEEDED,
    /** HC-05: starts in the past. */
    PAST_TIME,
    /** HC-07: planned duration exceeds the remaining duration. */
    DURATION_EXCEEDED,
    /** HC-08: task is not TODO/IN_PROGRESS of the user or an ACCEPTED assignment. */
    TASK_NOT_SCHEDULABLE,
    /**
     * HC-04 at confirm: the stored session is not within one calendar day of the user's current timezone
     * (e.g. the timezone changed after the edit). Approved by TV1 as a ConflictCode so confirm stays 409 (BR-11);
     * a bad range in an edit request is still the request-level {@link ErrorCode#INVALID_TIME_RANGE} (400).
     */
    INVALID_TIME_RANGE
}
