package group16.webapp.modules.task.api;

/** Task lifecycle (design doc TaskStatus). Only TODO/IN_PROGRESS tasks can be scheduled (HC-08). */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
