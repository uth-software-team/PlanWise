package group16.webapp.modules.assignment.api;

import java.util.Set;

/**
 * Public contract of the assignment module for other modules. Deliberately minimal: the assignment module
 * calls TaskApi, so the task module must not call back into this module (CG-10). Implemented in AS-02.
 */
public interface AssignmentApi {

    /** Ids of the tasks with an ACCEPTED assignment for this assignee (HC-08; used by SC-05b and SC-07). */
    Set<Long> acceptedTaskIds(Long userId);
}
