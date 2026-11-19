package group16.webapp.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Editable task draft returned by the AI parser (design doc TaskDraftDTO); nothing is saved until the user
 * submits POST /v1/tasks. priority/category are strings until TK-01 publishes the task enums; the allowed
 * values are the design doc's TaskPriority/TaskCategory and must stay stable.
 */
public record TaskDraftResponse(
        String title,
        Instant deadline,
        @Schema(allowableValues = {"LOW", "MEDIUM", "HIGH", "URGENT"}) String priority,
        @Schema(allowableValues = {"STUDY", "WORK", "PERSONAL", "PROJECT", "OTHER"}) String category,
        Integer estimatedDurationMinutes
) {
}
