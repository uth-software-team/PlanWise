package group16.webapp.modules.task.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Public contract of the task module for other modules (CG-06). Implemented in TK-04.
 * The task module never calls the assignment module (CG-10): callers pass accepted task ids in.
 */
public interface TaskApi {

    /**
     * The user's personal TODO/IN_PROGRESS non-deleted tasks, plus those of {@code acceptedGroupTaskIds} that are
     * TODO/IN_PROGRESS and not deleted (HC-08). The caller gets the accepted ids from AssignmentApi.
     */
    List<SchedulableTask> schedulableTasks(Long userId, Collection<Long> acceptedGroupTaskIds);

    /** Creates the single tasks row of a group task (status TODO) and returns its id (AS-01). */
    Long createGroupTask(Long groupId, Long creatorUserId, NewGroupTaskCommand command);

    /** Summaries of the given tasks; soft-deleted and missing tasks are left out. */
    List<TaskSummary> summaries(Collection<Long> taskIds);

    /** Group of a non-deleted group task; empty for personal, deleted or missing tasks (AS-01 reassignment). */
    Optional<Long> findActiveGroupTaskGroupId(Long taskId);
}
