# Recovery State — TV1 takeover of TV2–TV4 tasks

Persistent state of the takeover approved by TV1 (2026-11-28). Summary pointer lives in `docs/PROJECT_STATE.md`; details only here.
Each recovery task needs its own TV1 approval; Claude never commits or pushes (D13).

## Baseline

- Baseline: `origin/develop` 1a91375e6de7e5390a92e60530bc1be1eb45267e (2026-11-28, after #51)
- Phase: 4 (implementation, one task at a time)
- Strategy A (approved in principle): new branch from `origin/develop`, TV1 cherry-picks the original commit (`-x`, author kept), fixes on top as separate commits, new PR by TV1; the original PR is closed manually by TV1 after merge. Never push to another member's branch.
- Scope decisions (TV1, 2026-11-28): C-4 conditionally approved (FakeAiClient while E6 is open; keep Gemini-compatible design; fake output must be marked as fake; no claim of real Gemini). C-5 not approved. Group task status: option (a), no new endpoint in MVP (known limitation: an ACCEPTED group task stays TODO and keeps being scheduled until revoked).

## Recovery tasks

| ID | WBS | Owner | Branch / PR | Status | Notes |
|---|---|---|---|---|---|
| R-01 | TK-02, TK-03, TK-04 | TV3 | `feature/task-crud-takeover` (from 1a91375); original PR #35 | IMPLEMENTED | Awaiting TV1 commit/push/PR |
| R-02 | AV-02, AV-04, AV-03, CAL-01, AV-05 | TV3 | PR #36 + #39 (one branch planned) | TODO | #36 alone breaks fixed events; merge together |
| R-03 | AS-01, AS-02, AS-PG | TV4 | `feature/assignment-takeover` (worktree `../PlanWise-r03`, from 78549de) | IMPLEMENTED | Awaiting TV1 commit/push/PR |
| R-04 | SC-05b | TV2 | — | TODO | `ScheduleService` already reads `AssignmentApi` via ObjectProvider; with R-03 the accepted ids reach `schedulableTasks`. Remaining: end-to-end generate test with a real ACCEPTED task |
| R-05 | AS-PG (+ pg tests for task/availability) | TV4 | — | TODO | |
| R-06 | AI-01, AI-02 | TV4 | — | TODO | C-4 applies |
| R-07 | QA-01 | TV1 | — | TODO | After R-01..R-06 |
| R-08 | FE-TK, FE-AV, FE-GR-01, FE-AS, FE-AI | TV3, TV4 | — | TODO | |
| R-09 | IT-DRY, IT-02 | all | — | TODO | `docs/e2e/demo-flows.md` (PR #54) |

Status: TODO / IN_PROGRESS / IMPLEMENTED / VERIFIED / BLOCKED.

## R-01 — TK-02/03/04 (takeover of PR #35)

- Preserved commit: a5da7cc (Phan Ky Khoi) → cherry-picked as 2b7cf72 by TV1; tree == merge-tree simulation bf87bdf; no conflict. PR #35 head unchanged since review (a5da7cc, 03:43Z).
- Approved scope (TV1): F-1..F-4 + O-1.
  - F-1 (MEDIUM, D15): `/v1/tasks` list and GET/PUT/PATCH/DELETE exclude group tasks (`group_id IS NULL`), even when the caller owns them → 404.
  - F-2 (LOW): reverted 4 mojibake comment lines in `ScheduleServiceIntegrationTests`; `@Primary` kept (two `TaskApi` beans).
  - F-3 (LOW): test uses a real group created through `POST /v1/groups` instead of id 77. FK itself is covered on pg by `GroupAssignmentAiMigrationPgTests.groupTaskWithUnknownGroupIsRejected`.
  - F-4: D15 test, PUT added to the other-user isolation test, `findActiveGroupTaskGroupId(personal)` empty.
  - O-1: accepted ids only match group tasks (`t.groupId IS NOT NULL`).
- Verified not defects (excluded): free status transitions (design doc has no transition rules), no `@Max` on duration (BR-02 only `> 0`), PUT replacing `actualDurationMinutes` (full update).
- Business rules checked: BR-01, BR-02, BR-03, HC-08, D15; owner isolation (404); `STALE_VERSION`.
- Evidence (2026-11-28 19:25 +07, working tree on 2b7cf72 + uncommitted fix):
  - New tests fail without the `main/` fix (stash): `personalEndpointsExcludeGroupTasksOwnedByTheCaller`, `taskApiSupportsSchedulableTasksGroupCreationAndSummaries` → 2/5 failed; pass with it.
  - `./gradlew --offline --rerun-tasks build pgTest`: 9/9 tasks executed, BUILD SUCCESSFUL. XML: `test` 27 files, 376 tests, 0 failures/errors/skipped (12:25:41–12:25:53Z); `pgTest` 7 files, 169 tests, 0 failures/errors/skipped (12:25:53–12:26:01Z). Count vs develop (379): the 501 stub cases were replaced by real task tests.
  - IDE diagnostics: none in changed files.
- Remaining: CI on the new PR; peer review (code is TV3's; TV2/TV4 review recommended even if D12 applies); close #35 after merge.

## R-03 — AS-01/AS-02/AS-PG

- TV1 choices (2026-11-29): assignee not an ACTIVE member or account inactive → 400 INVALID_INPUT; leader may assign to self; group list via new `TaskApi.groupTaskIds` (no cross-module SQL).
- Behaviour: rights per request (`requireLeader`, D16); create with newTask or taskId (personal/deleted/missing → 404); BR-04 pre-check + V8 index → 409 ASSIGNMENT_ALREADY_ACTIVE; accept/decline/revoke are conditional updates (stale → 409 DATA_CONFLICT; non-assignee → 403); decline reason stripped; revoke keeps respondedAt. No notification (P1 cut).
- Files: assignment entity/repository/service(+impl, implements `AssignmentApi`), controller wired; `TaskApi`/`TaskService`/`TaskRepository` + `groupTaskIds`; test fakes updated, `@Primary` on the test `AssignmentApi` in `ScheduleServiceIntegrationTests`; stub test renamed `AssignmentControllerContractTests` (501 case removed); new `AssignmentControllerTests` (15, incl. AS-T1), `AssignmentServiceRacePgTests` (index path), 3 AS-PG cases in `GroupAssignmentAiMigrationPgTests`.
- Known limitations: accept/decline does not re-check that the assignee is still an ACTIVE member; an ACCEPTED group task stays TODO (option (a)).

## Blockers

- Unconfirmed: whether TV3/TV4 have unpushed work (TV1 to confirm before each takeover).

## Next recommended action

TV1 commits and pushes R-01, opens the PR; then approve the R-02 plan.
