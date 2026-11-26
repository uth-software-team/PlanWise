package group16.webapp.modules.group.entity;

import group16.webapp.common.entity.BaseEntity;
import group16.webapp.modules.group.api.GroupRole;
import group16.webapp.modules.group.api.MembershipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A user's membership in one group (V7 {@code group_members}, one row per group and user). The table has no
 * created_at/updated_at columns, so this extends {@link BaseEntity}.
 */
@Entity
@Table(name = "group_members")
public class GroupMember extends BaseEntity {

    @Column(name = "group_id", nullable = false, updatable = false)
    private Long groupId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private GroupRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private MembershipStatus status;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    protected GroupMember() {}

    /** A new ACTIVE membership. */
    public GroupMember(Long groupId, Long userId, GroupRole role, Instant joinedAt) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
        this.status = MembershipStatus.ACTIVE;
        this.joinedAt = joinedAt;
    }

    public void changeRole(GroupRole role) {
        this.role = role;
    }

    /** Leaving or removal: the row stays (group, user, role and joinedAt are preserved). */
    public void deactivate(Instant now) {
        this.status = MembershipStatus.INACTIVE;
        // V7 requires left_at >= joined_at.
        this.leftAt = now.isBefore(joinedAt) ? joinedAt : now;
    }

    /** Rejoining reuses the row: always MEMBER (a former LEADER role is not restored), joinedAt reset. */
    public void reactivate(Instant now) {
        this.role = GroupRole.MEMBER;
        this.status = MembershipStatus.ACTIVE;
        this.joinedAt = now;
        this.leftAt = null;
    }

    public Long getGroupId() { return groupId; }
    public Long getUserId() { return userId; }
    public GroupRole getRole() { return role; }
    public MembershipStatus getStatus() { return status; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLeftAt() { return leftAt; }
}
