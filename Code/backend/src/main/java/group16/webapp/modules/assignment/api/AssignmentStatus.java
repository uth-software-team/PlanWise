package group16.webapp.modules.assignment.api;

/** Assignment lifecycle (design doc AssignmentStatus), independent of the task's own status. */
public enum AssignmentStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    REVOKED
}
