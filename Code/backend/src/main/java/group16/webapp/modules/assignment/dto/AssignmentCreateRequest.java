package group16.webapp.modules.assignment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Leader assigns a group task (TV1 decision 2026-11-19): {@code assigneeUserId} plus exactly one of
 * {@code newTask} (first assignment: the service creates one tasks row via TaskApi.createGroupTask) or
 * {@code taskId} (reassignment: the group is derived from the existing task). Any other combination is
 * 400 INVALID_INPUT.
 */
public record AssignmentCreateRequest(
        @NotNull Long assigneeUserId,
        @Valid NewGroupTask newTask,
        Long taskId
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Provide exactly one of newTask or taskId")
    public boolean isExactlyOneTaskSource() {
        return (newTask == null) != (taskId == null);
    }

    /**
     * A new group task. priority/category are strings until TK-01 publishes the task enums; the allowed
     * values are the design doc's TaskPriority/TaskCategory and must stay stable.
     */
    public record NewGroupTask(
            @NotNull Long groupId,
            @NotBlank @Size(max = 255) String title,
            String description,
            Instant deadline,
            @NotNull @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT")
            @Schema(allowableValues = {"LOW", "MEDIUM", "HIGH", "URGENT"}) String priority,
            @NotNull @Pattern(regexp = "STUDY|WORK|PERSONAL|PROJECT|OTHER")
            @Schema(allowableValues = {"STUDY", "WORK", "PERSONAL", "PROJECT", "OTHER"}) String category,
            @NotNull @Positive Integer estimatedDurationMinutes
    ) {
    }
}
