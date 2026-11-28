# Project State

Persistent development state. Keep concise, evidence-based, and up to date.

## Context

- Project: PlanWise AI (task management + smart scheduling web app)
- Stack: Spring Boot backend (Java 25, Spring Boot 4.1.x, Gradle 9.8, PostgreSQL/H2); React frontend
- Architecture: See CLAUDE.md, Code/backend/CLAUDE.md, Code/frontend/CLAUDE.md
- Branch: `develop` = origin/develop 5d03acb (PR #48 merged), local synced. Open TV1 branches: `fix/schedule-remaining-minutes` (827b9f0, pushed, no PR), `fix/auth-jwt-hardening` and `fix/frontend-index-and-token-bar` (local only, not pushed)
- Last reviewed commit: 827b9f0 (build/pgTest run; diff not reviewed in this pass)
- Current milestone: Priorities 2–3 (Tasks, Availability/Fixed Events) are in open PRs #35/#36/#39, not merged. Deadline 2026-12-03 (5 days)
- Updated: 2026-11-28 (session closeout)

## Progress

| ID  | Feature / Task | Status | Evidence | Dependencies |
| --- | -------------- | ------ | -------- | ------------ |
| GOV-01 | Governance preflight (WORKFLOW §9), read-only | VERIFIED | Session 2026-11-28: hooks, branch, `gh` preflight 200 as `kietit2k6` | — |
| GOV-02 | Move work off iCloud-synced clone to a non-iCloud clone | VERIFIED | Branch fetched from old clone, HEAD b09c5e0 matched; 6 iCloud duplicate files were byte-identical to tracked originals and deleted from old clone | — |
| GOV-03 | P-RO policy: deny external-write tools and bare `git -C * push` | IMPLEMENTED | Merged in fcf7919 (PR #43); `.claude/settings.json` sha256 `5b26a96a3cafbfccf70c29dbca926ec8e8bf6632f267ce0e72fbfcba1a0c4f34`; policy behavior not yet tested | GOV-06 |
| GOV-04 | Update pr-review skill | IMPLEMENTED | Merged in fcf7919 (PR #43) | — |
| GOV-05 | Codex config: `sandbox_mode = "read-only"`, `approval_policy = "never"`, project path updated (outside repo) | IMPLEMENTED | Config parsed OK; behavior not tested. A trust entry for the old iCloud clone path was re-added later (not by Claude) | GOV-07 |
| GOV-06 | Fixture A4 rerun with new settings hash | BLOCKED | Needs TV1, new session in the non-iCloud clone | GOV-03 |
| GOV-07 | Fixture A5 rerun (Codex) | BLOCKED | Needs TV1 | GOV-05 |
| GOV-08 | PR #43 post-merge exception (WORKFLOW §7.5, option a) | IN_PROGRESS | Merged by `kiet232k6` with 0 reviews, no `WAIVE_PEER_REVIEW`. S=fcf7919 has 1 parent == D=6d59f3a; tree(S) == tree(H=94a4a06) == a111c56; tree(M) inferred equal (D ancestor of H, CI log not checked). Build/pgTest on tree(S) passed | Codex review `git diff M S` (TV1) |
| GOV-09 | PR #44 post-merge exception (WORKFLOW §7.5, option a) | IN_PROGRESS | Merged by `kiet232k6` with 0 reviews, no `WAIVE_PEER_REVIEW`. S=4b7e602 has 1 parent == D=fcf7919; tree(S) == tree(H=646a8e8) == 99ac6f3; tree(M) inferred equal (D ancestor of H, CI log not checked). Diff D..H: `docs/PROJECT_STATE.md` only. Build/pgTest on tree(S) passed (fresh `--rerun-tasks`). Not confirmed that CI `test` finished before merge | Codex review `git diff M S` (TV1) |
| GOV-10 | PR #45 post-merge exception (WORKFLOW §7.5, option a) | IN_PROGRESS | Merged by `kiet232k6` with 0 reviews, no `WAIVE_PEER_REVIEW`; CI 4/4 passed before merge. S=56d4364 has 1 parent == D=4b7e602; tree(S) == tree(H=424a27d) == 9744090; tree(M) inferred equal (D ancestor of H, CI log not checked). Diff D..H: `docs/PROJECT_STATE.md` only. Build/pgTest on tree(S) passed (fresh `--rerun-tasks`) | Codex review `git diff M S` (TV1) |
| GOV-11 | PR #46 post-merge exception (WORKFLOW §7.5, option a) | IN_PROGRESS | Merged by `kiet232k6` with 0 reviews, no `WAIVE_PEER_REVIEW`; CI 4/4 passed before merge. S=cdea36f has 1 parent == D=56d4364; tree(S) == tree(H=d004266) == 4436051; tree(M) inferred equal (D ancestor of H, CI log not checked). Diff D..H: `docs/PROJECT_STATE.md` only. Build/pgTest on tree(S) passed (fresh `--rerun-tasks`) | Codex review `git diff M S` (TV1) |

| GOV-12 | PR #47 post-merge exception (WORKFLOW §7.5) | TODO | Merged by `kiet232k6` as f6f602d, `reviewDecision` REVIEW_REQUIRED (5th in a row). Tree/CI/build not checked this session | Admin-bypass root-cause decision (TV1) |
| GOV-13 | PR #48 post-merge exception (WORKFLOW §7.5) | TODO | Merged by `kiet232k6` as 5d03acb (2026-11-28 03:54Z), head 8e7db4b (not 7323c47), `reviewDecision` REVIEW_REQUIRED, 0 reviews, no `ACCEPT`/`WAIVE_PEER_REVIEW` comment (6th in a row). Tree/CI not checked | Admin-bypass root-cause decision (TV1) |
| API-01 | Max query range 366 days for group availability + schedule generate (PR #48, TV1) | IMPLEMENTED | Merged in 5d03acb (head 8e7db4b): `DateUtils.exceedsMaxQueryRange`, checks in `GroupAvailabilityServiceImpl` and `ScheduleService.generateProposal`, edit-session batch span cap, OpenAPI + `schema.d.ts`. No build/pgTest on 5d03acb itself; its child 827b9f0 passed (see SC-FIX-01). Merged without review (GOV-13) | GOV-13 |
| SC-FIX-01 | Scope remaining-minutes query and lock batch before editing sessions (`fix/schedule-remaining-minutes`, TV1) | IMPLEMENTED | 827b9f0 (1 commit on 5d03acb, pushed, no PR): `ScheduleSessionRepository`, `ScheduleService`, new `ScheduleServiceIntegrationTests` cases. 2026-11-28 `./gradlew --rerun-tasks build pgTest` on tree 5550131: H2 377/377, pg 167/167, 0 failures/errors/skipped. Link to R48-1 not confirmed | TV1 PR, review, ACCEPT |
| TK-01 | Personal task CRUD + TaskApi (PR #35, Khoi12vn) | IN_PROGRESS | CI 4/4 SUCCESS. TV1 review 2026-11-28 03:02Z: [MEDIUM] group tasks leak into personal endpoints. TV1 already decided: D15 (Code/backend/CLAUDE.md) in PR #48 = personal only, group task id 404. No commits since review | Author fix |
| AV-02 | Availability rule CRUD + interval expansion (PR #36, Khoi12vn) | IN_PROGRESS | CI 4/4 SUCCESS. TV1 review: [HIGH] empty `fixedEventOccurrences` lets scheduling ignore fixed events (merge #39 right after, or throw NOT_IMPLEMENTED); [LOW] per-day loop. No commits since review | Author fix; merge order with #39 |
| AV-03 | Fixed events + calendar (PR #39, Khoi12vn) | IN_PROGRESS | CI 4/4 SUCCESS. TV1 review: [MEDIUM] WEEKLY expansion untested (4 cases); [MEDIUM] unbounded calendar range (to use API-01 helper). No commits since review | Author fix; API-01 merged first (helper) |

Status: TODO / IN_PROGRESS / IMPLEMENTED / VERIFIED / BLOCKED / DEFERRED.

## Quality

Checked on tree 5550131 (= 827b9f0, `fix/schedule-remaining-minutes`), 2026-11-28 ~11:30 +07, `./gradlew --rerun-tasks build pgTest` (9/9 tasks executed):

- Build: PASS
- Lint: NOT_RUN (backend only)
- Type check: NOT_RUN (backend only)
- Tests: PASS (`test` H2: 377 tests, 0 failures, 0 errors, 0 skipped; `pgTest` PostgreSQL 16: 167 tests, 0 failures, 0 errors, 0 skipped)
- CI: not yet run on 827b9f0 (no PR)
- Security audit: See docs/security/AUDIT.md

## Current Task

- ID: GOV-08 / GOV-09 / GOV-10 / GOV-11 (governance maintenance, no PW Issue)
- Objective: Close the PR #43–#46 post-merge exceptions per WORKFLOW §7.5 option (a)
- Status: IN_PROGRESS
- Acceptance criteria: Codex review of `git diff M S` done for each PR (expected empty); §7.5 record posted on PR #43–#46 by TV1

## Next Task

- ID: SC-FIX-01, then SEC-01 (TV1 branches)
- Priority: Highest (deadline 2026-12-03; product work outranks GOV-06/07)
- Objective: TV1 opens a PR for `fix/schedule-remaining-minutes`; pushes `fix/auth-jwt-hardening` and `fix/frontend-index-and-token-bar` (local only, at risk of loss) and opens their PRs
- Reason: Only ready, unblocked product items; API-01 (#48) is merged, so #39 can now reuse the helper
- Dependencies: TV1 push/PR (D13); peer review or D12 waiver; Codex review
- Acceptance criteria: CI 4/4 on each head; head/base checks per WORKFLOW §7.1; TV1 ACCEPT; merged; post-merge build/pgTest passes
- After: GOV-06/07 (A4/A5 reruns) once GOV-08..13 close
- Approval: PENDING

## Blockers & Decisions

- 2026-11-28, TV1: auto permission mode accepted for the 2026-11-28 session only. WORKFLOW §9 still says `default`; no docs PR yet.
- 2026-11-28, TV1: risk accepted: `git push` credentials remain in `osxkeychain` (HTTPS remote, no SSH keys). Deny-list bypasses (env/sh -c/alias/script) are not blockable by settings.
- 2026-11-28, TV1: PR #43, #44, #45 and #46 merged without peer review or D12 waiver; TV1 chose WORKFLOW §7.5 option (a) (build/pgTest on S + Codex review) for each, not revert.
- Open (2026-11-28): deadline risk. Tasks/Availability/Fixed Events (#35/#36/#39) not merged 5 days before 2026-12-03; scheduling needs them. Merge order (PR #48 body): #48 → #36 → #39 → #35. R48-1 (uncapped edit/confirm expansion) must be fixed before #36 merges. Scope cuts not yet proposed.
- Open: merges by `kiet232k6` pass `REVIEW_REQUIRED` through the ruleset admin bypass (6 times in a row, #43–#48; each state-recording PR created a new exception). Root-cause options for TV1: (i) mentor submits an Approve review on GitHub at the PR head and TV1 merges; (ii) TV1 narrows the ruleset bypass actor; (iii) record a team decision via a WORKFLOW docs PR. Avoid opening PRs only to record these exceptions.
- Open: skill `schedule` and `Agent` with `isolation: "remote"` cannot be denied by tool name.
- Open: non-iCloud clone has no `/.claude/settings.local.json` entry in `.git/info/exclude`; add it before creating that file.
- Open: read-only `gh` token expires 2026-12-18.
- 2026-11-28 closeout: merged branches from #42–#48 no longer exist locally or on origin (checked after `git fetch --prune`). Open PRs #35/#36/#39 are CHANGES_REQUESTED, CI 4/4, 7–12 commits behind `develop`.

## Resume

- Completed: GOV-01, GOV-02 (verified); GOV-03, GOV-04 (merged in fcf7919); GOV-05 (applied outside repo); API-01 merged (5d03acb, unreviewed, GOV-13); local `develop` synced
- Pending: SC-FIX-01 PR; push + PRs for SEC-01 branches; R48-1 follow-up (status unconfirmed); author fixes on #35/#36/#39; root-cause decision on the admin bypass; GOV-08 to GOV-13 Codex reviews + §7.5 records; A4/A5 reruns; token renewal before 2026-12-18
- Next action: TV1 pushes the two local-only fix branches and opens PRs for all three fix branches; TV1 confirms whether 827b9f0 covers R48-1. Governance items after.

Update after each completed task. Revalidate stale information when relevant code changes. Never mark unverified work as VERIFIED. Avoid duplicate analysis.
