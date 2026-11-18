package group16.webapp.modules.notification.api;

/**
 * Public contract for other modules (assignment, schedule, ...) to raise a notification (BR-13).
 * Parameters mirror the ERD table {@code notifications}.
 */
public interface NotificationApi {

    /**
     * Records a notification for one recipient.
     *
     * @param recipientUserId user who receives it (required)
     * @param type            business event (required)
     * @param title           short title shown to the recipient
     * @param content         body text shown to the recipient
     * @param referenceType   optional kind of the related record, e.g. "TASK_ASSIGNMENT"
     * @param referenceId     optional id of the related record
     */
    void notify(Long recipientUserId, NotificationType type, String title, String content,
                String referenceType, Long referenceId);
}
