package group16.webapp.modules.schedule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Confirms a whole PROPOSED batch (design doc ConfirmRequest, BR-11). {@code version} is the batch version. */
public record ConfirmRequest(
        @NotNull @PositiveOrZero Integer version
) {
}
