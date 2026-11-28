-- V12: rollback of D16 (one ACTIVE LEADER per group). Drops the V11 partial unique index so the previous
-- multi-LEADER group rules work again. V11 stays in place (applied migrations are never edited or removed).
-- Runbook: docs/db/single-leader-cutover.md, steps R1-R7. To re-enable, add a new migration that recreates the
-- index and run M0-M6 again. Never edit this file once applied.

DROP INDEX IF EXISTS uk_group_members_one_active_leader;
