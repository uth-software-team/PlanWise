package group16.webapp.modules.task.dto;

import group16.webapp.modules.task.api.TaskCategory;
import group16.webapp.modules.task.api.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * New personal task (design doc TaskCreateRequest; BR-01, BR-02). The deadline is optional; a missing
 * description is stored as an empty string; new tasks start as TODO.
 */
public record TaskCreateRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        Instant deadline,
        @NotNull TaskPriority priority,
        @NotNull TaskCategory category,
        @NotNull @Positive Integer estimatedDurationMinutes
) {
}
