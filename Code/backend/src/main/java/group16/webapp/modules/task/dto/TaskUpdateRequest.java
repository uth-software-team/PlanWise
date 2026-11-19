package group16.webapp.modules.task.dto;

import group16.webapp.modules.task.api.TaskCategory;
import group16.webapp.modules.task.api.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Full update of a task (design doc TaskUpdateRequest). {@code version} is the value last read (409 STALE_VERSION). */
public record TaskUpdateRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        Instant deadline,
        @NotNull TaskPriority priority,
        @NotNull TaskCategory category,
        @NotNull @Positive Integer estimatedDurationMinutes,
        @PositiveOrZero Integer actualDurationMinutes,
        @NotNull @PositiveOrZero Integer version
) {
}
