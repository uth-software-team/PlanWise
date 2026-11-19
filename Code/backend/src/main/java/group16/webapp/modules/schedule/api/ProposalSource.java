package group16.webapp.modules.schedule.api;

/**
 * Origin of a proposal batch (V10 ck_schedule_proposals_source; TV1 decision 2026-11-19). The MVP only creates
 * ENGINE batches (SC-05a); AI is reserved for applying AI conflict advice and is not produced yet.
 */
public enum ProposalSource {
    ENGINE,
    AI
}
