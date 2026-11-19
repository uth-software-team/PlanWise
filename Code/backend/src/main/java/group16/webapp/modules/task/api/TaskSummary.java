package group16.webapp.modules.task.api;

import java.time.Instant;

/** Small, non-sensitive view of a task for other modules (e.g. assignment lists, calendar titles). */
public record TaskSummary(
        Long taskId,
        Long groupId,
        String title,
        Instant deadline,
        TaskPriority priority,
        TaskStatus status
) {
}
