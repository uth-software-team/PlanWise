package group16.webapp.modules.assignment.repository;

import group16.webapp.modules.assignment.api.AssignmentStatus;
import group16.webapp.modules.assignment.entity.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Transitions are conditional updates: they only change a row that is still in the expected status, so a stale
 * transition updates 0 rows (409 DATA_CONFLICT in the service). Bulk updates bypass JPA auditing, so they set
 * {@code updatedAt} themselves.
 */
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    boolean existsByTaskIdAndStatusIn(Long taskId, Collection<AssignmentStatus> statuses);

    List<TaskAssignment> findByAssigneeUserIdOrderByCreatedAtDescIdDesc(Long assigneeUserId);

    List<TaskAssignment> findByAssigneeUserIdAndStatusOrderByCreatedAtDescIdDesc(
            Long assigneeUserId, AssignmentStatus status);

    List<TaskAssignment> findByTaskIdInOrderByCreatedAtDescIdDesc(Collection<Long> taskIds);

    List<TaskAssignment> findByTaskIdInAndStatusOrderByCreatedAtDescIdDesc(
            Collection<Long> taskIds, AssignmentStatus status);

    @Query("""
            SELECT a.taskId FROM TaskAssignment a
            WHERE a.assigneeUserId = :userId
              AND a.status = group16.webapp.modules.assignment.api.AssignmentStatus.ACCEPTED
            """)
    List<Long> findAcceptedTaskIds(@Param("userId") Long userId);

    /** PENDING -> ACCEPTED by the assignee; sets respondedAt. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TaskAssignment a
            SET a.status = group16.webapp.modules.assignment.api.AssignmentStatus.ACCEPTED,
                a.respondedAt = :now, a.updatedAt = :now
            WHERE a.id = :id AND a.assigneeUserId = :userId
              AND a.status = group16.webapp.modules.assignment.api.AssignmentStatus.PENDING
            """)
    int accept(@Param("id") Long id, @Param("userId") Long userId, @Param("now") Instant now);

    /** PENDING -> DECLINED by the assignee with a reason (BR-05); sets respondedAt. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TaskAssignment a
            SET a.status = group16.webapp.modules.assignment.api.AssignmentStatus.DECLINED,
                a.declineReason = :reason, a.respondedAt = :now, a.updatedAt = :now
            WHERE a.id = :id AND a.assigneeUserId = :userId
              AND a.status = group16.webapp.modules.assignment.api.AssignmentStatus.PENDING
            """)
    int decline(@Param("id") Long id, @Param("userId") Long userId, @Param("reason") String reason,
                @Param("now") Instant now);

    /** PENDING/ACCEPTED -> REVOKED; respondedAt keeps its value, including NULL (TV1 decision 2026-11-19). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TaskAssignment a
            SET a.status = group16.webapp.modules.assignment.api.AssignmentStatus.REVOKED, a.updatedAt = :now
            WHERE a.id = :id
              AND a.status IN (group16.webapp.modules.assignment.api.AssignmentStatus.PENDING,
                               group16.webapp.modules.assignment.api.AssignmentStatus.ACCEPTED)
            """)
    int revoke(@Param("id") Long id, @Param("now") Instant now);
}
