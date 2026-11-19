package group16.webapp.modules.assignment.dto;

import group16.webapp.modules.assignment.api.AssignmentStatus;

import java.time.Instant;

/** Assignment as returned by every /v1/assignments endpoint (design doc AssignmentDTO). */
public record AssignmentResponse(
        Long id,
        AssignmentStatus assignmentStatus,
        Long assigneeUserId,
        Long assignedByUserId,
        String declineReason,
        Instant respondedAt,
        Instant createdAt,
        AssignmentTaskSummary task
) {
}
