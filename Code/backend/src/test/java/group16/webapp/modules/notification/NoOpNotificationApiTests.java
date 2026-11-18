package group16.webapp.modules.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import group16.webapp.modules.notification.api.NotificationApi;
import group16.webapp.modules.notification.api.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Other modules can inject NotificationApi before the real implementation (ND-01) exists. */
@SpringBootTest
@ActiveProfiles("test")
class NoOpNotificationApiTests {

	@Autowired
	private NotificationApi notificationApi;

	@Test
	void notificationApiIsInjectableAndNotifyDoesNotFail() {
		assertThat(notificationApi).isNotNull();
		assertThatCode(() -> notificationApi.notify(1L, NotificationType.TASK_ASSIGNED,
				"New task", "You have a new task", "TASK_ASSIGNMENT", 10L))
				.doesNotThrowAnyException();
	}
}
