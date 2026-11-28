-- V13: re-enables D16 (at most one ACTIVE LEADER per group, TV1 2026-11-29) after V12 dropped the V11 index when
-- the rollback PR #59 was merged by mistake. Same guard and index as V11; V11 and V12 stay unchanged.
-- The guard fails the migration (and Flyway rolls it back) while any group with ACTIVE members has a number of
-- ACTIVE LEADERs other than 1 (old multi-LEADER code ran between V12 and V13). Fix the data first with
-- docs/db/single-leader-cutover.md (M2-M4).
-- PostgreSQL only (LOCK TABLE, DO block, partial index). Never edit this file once applied.

-- Blocks concurrent membership writes between the check and the index creation.
LOCK TABLE group_members IN SHARE MODE;

DO $$
DECLARE
    bad text;
BEGIN
    SELECT string_agg(group_id::text, ', ' ORDER BY group_id) INTO bad
    FROM (
        SELECT group_id
        FROM group_members
        WHERE status = 'ACTIVE'
        GROUP BY group_id
        HAVING COUNT(*) FILTER (WHERE role = 'LEADER') <> 1
    ) v;
    IF bad IS NOT NULL THEN
        RAISE EXCEPTION 'V13: groups without exactly one ACTIVE LEADER: %', bad;
    END IF;
END $$;

-- INACTIVE rows keep their former role, so only ACTIVE LEADER rows are covered.
CREATE UNIQUE INDEX uk_group_members_one_active_leader
    ON group_members (group_id) WHERE role = 'LEADER' AND status = 'ACTIVE';
