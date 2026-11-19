package group16.webapp.modules.schedule.api;

/**
 * Lifecycle of one schedule session (design doc SessionStatus). Only PROPOSED sessions can be edited before
 * confirmation; CONFIRMED/COMPLETED sessions are never moved or overwritten by a new generate.
 */
public enum SessionStatus {
    PROPOSED,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
