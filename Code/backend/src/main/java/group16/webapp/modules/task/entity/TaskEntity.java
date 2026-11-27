package group16.webapp.modules.task.entity;

import group16.webapp.common.entity.AuditableEntity;
import group16.webapp.modules.task.api.TaskCategory;
import group16.webapp.modules.task.api.TaskPriority;
import group16.webapp.modules.task.api.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "tasks")
public class TaskEntity extends AuditableEntity {

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "deadline")
    private Instant deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 16)
    private TaskCategory category;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public TaskCategory getCategory() {
        return category;
    }

    public void setCategory(TaskCategory category) {
        this.category = category;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public Integer getActualDurationMinutes() {
        return actualDurationMinutes;
    }

    public void setActualDurationMinutes(Integer actualDurationMinutes) {
        this.actualDurationMinutes = actualDurationMinutes;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}