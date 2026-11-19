package group16.webapp.modules.schedule.api;

/** Why a task got no session in a generate that still succeeded (design doc UnscheduledReason, CG-07). */
public enum UnscheduledReason {
    /** The task deadline is already in the past. */
    DEADLINE_PASSED,
    /** No free slot in the window is long enough before the deadline. */
    INSUFFICIENT_TIME
}
