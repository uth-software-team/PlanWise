package group16.webapp.modules.task.api;

import java.time.Instant;

/** Data of a new group task created by a leader through the assignment module (AS-01). */
public record NewGroupTaskCommand(
        String title,
        String description,
        Instant deadline,
        TaskPriority priority,
        TaskCategory category,
        int estimatedDurationMinutes
) {
}
