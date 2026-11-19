package group16.webapp.modules.schedule.api;

/**
 * Lifecycle of a proposal batch (V10 ck_schedule_proposals_status; TV1 decision 2026-11-19). At most one PROPOSED
 * batch per user; a batch replaced by a new generate or discarded by the user becomes CANCELLED.
 */
public enum ProposalStatus {
    PROPOSED,
    CONFIRMED,
    CANCELLED
}
