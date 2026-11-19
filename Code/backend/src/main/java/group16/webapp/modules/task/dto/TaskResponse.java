package group16.webapp.modules.task.dto;

import group16.webapp.modules.task.api.TaskCategory;
import group16.webapp.modules.task.api.TaskPriority;
import group16.webapp.modules.task.api.TaskStatus;

import java.time.Instant;

/** Task as returned by the API (design doc TaskResponse). {@code overdue} is derived, never stored (BR-03). */
public record TaskResponse(
        Long id,
        Long groupId,
        String title,
        String description,
        Instant deadline,
        TaskPriority priority,
        TaskCategory category,
        TaskStatus status,
        Integer estimatedDurationMinutes,
        Integer actualDurationMinutes,
        boolean overdue,
        Integer version,
        Instant createdAt,
        Instant updatedAt
) {
}
