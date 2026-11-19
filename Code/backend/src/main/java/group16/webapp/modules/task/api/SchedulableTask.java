package group16.webapp.modules.task.api;

import java.time.Instant;

/** Scheduling input for one TODO/IN_PROGRESS task (CG-07); the schedule module derives the remaining duration. */
public record SchedulableTask(
        Long taskId,
        Instant deadline,
        TaskPriority priority,
        int estimatedDurationMinutes,
        Instant createdAt
) {
}
