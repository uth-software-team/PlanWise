package group16.webapp.modules.task.repository;

import group16.webapp.modules.task.api.TaskStatus;
import group16.webapp.modules.task.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<TaskEntity, Long>, JpaSpecificationExecutor<TaskEntity> {

    /** Personal task of the owner only (D15): group tasks are excluded even when the caller is their owner. */
    Optional<TaskEntity> findByIdAndOwnerUserIdAndGroupIdIsNullAndDeletedAtIsNull(Long id, Long ownerUserId);

    Optional<TaskEntity> findByIdAndDeletedAtIsNull(Long id);

    List<TaskEntity> findByIdInAndDeletedAtIsNull(Collection<Long> ids);

    /** Ids of a group's non-deleted tasks. */
    @Query("SELECT t.id FROM TaskEntity t WHERE t.groupId = :groupId AND t.deletedAt IS NULL")
    List<Long> findActiveIdsByGroupId(@Param("groupId") Long groupId);

    @Query("""
            SELECT t FROM TaskEntity t
            WHERE t.deletedAt IS NULL
              AND t.status IN :statuses
              AND ((t.ownerUserId = :userId AND t.groupId IS NULL)
                   OR (t.id IN :acceptedGroupTaskIds AND t.groupId IS NOT NULL))
            """)
    List<TaskEntity> findSchedulableWithGroupTasks(
            @Param("userId") Long userId,
            @Param("acceptedGroupTaskIds") Collection<Long> acceptedGroupTaskIds,
            @Param("statuses") Collection<TaskStatus> statuses);

    @Query("""
            SELECT t FROM TaskEntity t
            WHERE t.deletedAt IS NULL
              AND t.status IN :statuses
              AND t.ownerUserId = :userId
              AND t.groupId IS NULL
            """)
    List<TaskEntity> findPersonalSchedulable(
            @Param("userId") Long userId,
            @Param("statuses") Collection<TaskStatus> statuses);
}