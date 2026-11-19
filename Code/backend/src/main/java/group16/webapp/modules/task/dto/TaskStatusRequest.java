package group16.webapp.modules.task.dto;

import group16.webapp.modules.task.api.TaskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Status change (design doc StatusRequest). {@code version} is the value last read (409 STALE_VERSION). */
public record TaskStatusRequest(
        @NotNull TaskStatus status,
        @NotNull @PositiveOrZero Integer version
) {
}
