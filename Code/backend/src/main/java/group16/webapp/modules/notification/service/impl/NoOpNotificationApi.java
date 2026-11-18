package group16.webapp.modules.notification.service.impl;

import group16.webapp.modules.notification.api.NotificationApi;
import group16.webapp.modules.notification.api.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * MVP placeholder: notifications are a P1 feature (WBS ND-01), so callers can depend on
 * {@link NotificationApi} now and the real persistence replaces this class later.
 * Logs only the event type and reference ids, never the title or content.
 */
@Service
public class NoOpNotificationApi implements NotificationApi {

    private static final Logger log = LoggerFactory.getLogger(NoOpNotificationApi.class);

    @Override
    public void notify(Long recipientUserId, NotificationType type, String title, String content,
                       String referenceType, Long referenceId) {
        log.debug("Notification skipped (no-op): type={}, recipientUserId={}, referenceType={}, referenceId={}",
                type, recipientUserId, referenceType, referenceId);
    }
}
