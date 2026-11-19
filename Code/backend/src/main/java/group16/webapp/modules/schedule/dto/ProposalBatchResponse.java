package group16.webapp.modules.schedule.dto;

import group16.webapp.modules.schedule.api.ProposalStatus;
import group16.webapp.modules.schedule.api.SessionStatus;
import group16.webapp.modules.schedule.api.UnscheduledReason;

import java.time.Instant;
import java.util.List;

/**
 * One proposal batch of the caller (design doc ProposalBatchResponseDTO). {@code version} is the batch version that
 * every edit, delete and confirm request must send back. Per-session conflicts are not part of a success response:
 * they come in the data of 409 CONSTRAINT_VIOLATION (CG-05). Instants are UTC; intervals are half-open.
 */
public record ProposalBatchResponse(
        Long proposalId,
        int version,
        ProposalStatus status,
        Instant horizonStart,
        Instant horizonEnd,
        List<ProposedSession> sessions,
        List<UnscheduledTask> unscheduledTasks,
        Instant createdAt,
        Instant confirmedAt
) {

    /** One session of the batch; {@code version} is the session's own version, for display only. */
    public record ProposedSession(
            Long sessionId,
            Long taskId,
            String taskTitle,
            Instant startAt,
            Instant endAt,
            SessionStatus status,
            int version
    ) {
    }

    /** A task that got no session in this batch, with the minutes still missing. */
    public record UnscheduledTask(
            Long taskId,
            String taskTitle,
            UnscheduledReason reason,
            int missingMinutes
    ) {
    }
}
