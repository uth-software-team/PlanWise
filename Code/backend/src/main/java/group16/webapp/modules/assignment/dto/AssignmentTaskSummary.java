package group16.webapp.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Small task view embedded in an assignment (TV1 decision 2026-11-19). taskStatus is the task's own status,
 * separate from the assignment status. priority/taskStatus are strings until TK-01 publishes the task enums;
 * the allowed values below are the design doc's TaskPriority/TaskStatus and must stay stable.
 */
public record AssignmentTaskSummary(
        Long taskId,
        String title,
        Instant deadline,
        @Schema(allowableValues = {"LOW", "MEDIUM", "HIGH", "URGENT"}) String priority,
        @Schema(allowableValues = {"TODO", "IN_PROGRESS", "COMPLETED", "CANCELLED"}) String taskStatus
) {
}
