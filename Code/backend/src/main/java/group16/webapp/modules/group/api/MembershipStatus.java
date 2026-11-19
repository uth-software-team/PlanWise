package group16.webapp.modules.group.api;

/**
 * Membership state (TV1 decision 2026-11-19). A user who rejoins reactivates the same membership row;
 * a former LEADER role is never restored implicitly.
 */
public enum MembershipStatus {
    ACTIVE,
    INACTIVE
}
