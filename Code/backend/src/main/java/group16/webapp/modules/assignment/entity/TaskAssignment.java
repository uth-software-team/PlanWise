package group16.webapp.modules.assignment.entity;

import group16.webapp.common.entity.AuditableEntity;
import group16.webapp.modules.assignment.api.AssignmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One assignment of a group task to a member (V8 {@code task_assignments}). Status transitions are done by
 * conditional updates in {@link group16.webapp.modules.assignment.repository.TaskAssignmentRepository}, so this
 * entity only creates new PENDING rows.
 */
@Entity
@Table(name = "task_assignments")
public class TaskAssignment extends AuditableEntity {

    @Column(name = "task_id", nullable = false, updatable = false)
    private Long taskId;

    @Column(name = "assignee_user_id", nullable = false, updatable = false)
    private Long assigneeUserId;

    @Column(name = "assigned_by_user_id", nullable = false, updatable = false)
    private Long assignedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AssignmentStatus status;

    @Column(name = "decline_reason")
    private String declineReason;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected TaskAssignment() {}

    /** A new PENDING assignment (no response yet). */
    public TaskAssignment(Long taskId, Long assigneeUserId, Long assignedByUserId) {
        this.taskId = taskId;
        this.assigneeUserId = assigneeUserId;
        this.assignedByUserId = assignedByUserId;
        this.status = AssignmentStatus.PENDING;
    }

    public Long getTaskId() { return taskId; }
    public Long getAssigneeUserId() { return assigneeUserId; }
    public Long getAssignedByUserId() { return assignedByUserId; }
    public AssignmentStatus getStatus() { return status; }
    public String getDeclineReason() { return declineReason; }
    public Instant getRespondedAt() { return respondedAt; }
}
