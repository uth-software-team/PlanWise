package group16.webapp.modules.group.entity;

import group16.webapp.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A team group (V7 {@code team_groups}). The creator is stored as a raw user id (no cross-module relation). */
@Entity
@Table(name = "team_groups")
public class TeamGroup extends AuditableEntity {

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private Long createdByUserId;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    protected TeamGroup() {}

    public TeamGroup(Long createdByUserId, String groupName, String description) {
        this.createdByUserId = createdByUserId;
        this.groupName = groupName;
        this.description = description;
    }

    public void update(String groupName, String description) {
        this.groupName = groupName;
        this.description = description;
    }

    public Long getCreatedByUserId() { return createdByUserId; }
    public String getGroupName() { return groupName; }
    public String getDescription() { return description; }
}
