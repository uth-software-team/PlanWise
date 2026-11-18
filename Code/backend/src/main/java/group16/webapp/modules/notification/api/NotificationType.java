package group16.webapp.modules.notification.api;

/** Business event behind a notification (design doc enum "NotificationType"). */
public enum NotificationType {
    TASK_ASSIGNED,
    ASSIGNMENT_ACCEPTED,
    ASSIGNMENT_DECLINED,
    ASSIGNMENT_REVOKED,
    DEADLINE_REMINDER,
    TASK_OVERDUE,
    SCHEDULE_PROPOSAL_READY,
    SCHEDULE_CONFLICT
}
