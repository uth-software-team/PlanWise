# Project State

Persistent development state. Keep concise, evidence-based, and up to date.

## Context

- Project: PlanWise AI (task management + smart scheduling web app)
- Stack: Spring Boot backend (Java 25, Spring Boot 4.1.x, Gradle 9.8, PostgreSQL/H2); React frontend
- Architecture: See CLAUDE.md, Code/backend/CLAUDE.md, Code/frontend/CLAUDE.md
- Branch: `feature/common-contract-max-query-range` (HEAD 7323c47 = PR #48 head, pushed); origin/develop f6f602d (PR #47 merged). Local `develop` is stale (b09eb7e)
- Last reviewed commit: 7323c47 (diff vs f6f602d read; tests not rerun locally this session)
- Current milestone: Priorities 2–3 (Tasks, Availability/Fixed Events) are in open PRs #35/#36/#39, not merged. Deadline 2026-12-03 (5 days)
- Updated: 2026-11-28 (project-status pass)

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
| API-01 | Max query range 366 days for group availability + schedule generate (PR #48, TV1) | IMPLEMENTED | 7323c47: `DateUtils.exceedsMaxQueryRange`, checks in `GroupAvailabilityServiceImpl` and `ScheduleService.generateProposal`, OpenAPI + `schema.d.ts` re-exported, tests added. CI 4/4 SUCCESS on 7323c47. Commit msg reports H2 373/373, pg 167/167 (not rerun this session). Review: REVIEW_REQUIRED | Codex review + TV1 D12 waiver or peer review; TV1 ACCEPT |
| TK-01 | Personal task CRUD + TaskApi (PR #35, Khoi12vn) | IN_PROGRESS | CI 4/4 SUCCESS. TV1 review 2026-11-28 03:02Z: [MEDIUM] group tasks leak into personal endpoints. TV1 already decided: D15 (Code/backend/CLAUDE.md) in PR #48 = personal only, group task id 404. No commits since review | Author fix |
| AV-02 | Availability rule CRUD + interval expansion (PR #36, Khoi12vn) | IN_PROGRESS | CI 4/4 SUCCESS. TV1 review: [HIGH] empty `fixedEventOccurrences` lets scheduling ignore fixed events (merge #39 right after, or throw NOT_IMPLEMENTED); [LOW] per-day loop. No commits since review | Author fix; merge order with #39 |
| AV-03 | Fixed events + calendar (PR #39, Khoi12vn) | IN_PROGRESS | CI 4/4 SUCCESS. TV1 review: [MEDIUM] WEEKLY expansion untested (4 cases); [MEDIUM] unbounded calendar range (to use API-01 helper). No commits since review | Author fix; API-01 merged first (helper) |
| SEC-01 | Security audit fixes SA-02 (reject placeholder JWT secret) + SA-04 (JWT issuer check), branch `fix/auth-jwt-hardening` | IMPLEMENTED | 2026-11-28 uncommitted on base 5d03acb: H2 379/379, pgTest 167/167 (`--rerun-tasks`). SA-01 deferred (TV1). Details: docs/security/AUDIT.md | TV1 commit + PR, review, ACCEPT |

Status: TODO / IN_PROGRESS / IMPLEMENTED / VERIFIED / BLOCKED / DEFERRED.

## Quality

Checked on tree 4436051 (= cdea36f), 2026-11-28 09:43 +07, `./gradlew --offline --rerun-tasks build pgTest` (9/9 tasks executed):

- Build: PASS
- Lint: NOT_RUN locally (CI `frontend` job passed on d004266)
- Type check: NOT_RUN locally (CI `frontend` job passed on d004266)
- Tests: PASS (`test` H2: 364 tests, 0 failures, 0 errors, 0 skipped; `pgTest` PostgreSQL 16: 167 tests, 0 failures, 0 errors, 0 skipped)
- CI on d004266: frontend, gitflow, pg, test all pass
- Security audit: See docs/security/AUDIT.md

## Current Task

- ID: GOV-08 / GOV-09 / GOV-10 / GOV-11 (governance maintenance, no PW Issue)
- Objective: Close the PR #43–#46 post-merge exceptions per WORKFLOW §7.5 option (a)
- Status: IN_PROGRESS
- Acceptance criteria: Codex review of `git diff M S` done for each PR (expected empty); §7.5 record posted on PR #43–#46 by TV1

## Next Task

- ID: API-01 (PR #48)
- Priority: Highest (deadline 2026-12-03; product work outranks GOV-06/07)
- Objective: Finish PR #48 so #39 can reuse `DateUtils.exceedsMaxQueryRange`
- Reason: Only ready, unblocked product item; #39 depends on it
- Dependencies: Codex review (TV1); TV2 + TV4 review required by 01_CONTRACT_GATES.md:6 (D12 waiver does not cover it). Claude review 2026-11-28: no blocking code issue (docs/reviews/PR_REVIEW.md)
- Acceptance criteria: Codex review has no unresolved HIGH/MEDIUM; CI 4/4 on head 7323c47 (done); head/base checks per WORKFLOW §7.1; TV1 ACCEPT; merged; post-merge build/pgTest on squash commit passes
- After: GOV-06/07 (A4/A5 reruns) once GOV-08..12 close
- Approval: PENDING

## Blockers & Decisions

- 2026-11-28, TV1: auto permission mode accepted for the 2026-11-28 session only. WORKFLOW §9 still says `default`; no docs PR yet.
- 2026-11-28, TV1: risk accepted: `git push` credentials remain in `osxkeychain` (HTTPS remote, no SSH keys). Deny-list bypasses (env/sh -c/alias/script) are not blockable by settings.
- 2026-11-28, TV1: PR #43, #44, #45 and #46 merged without peer review or D12 waiver; TV1 chose WORKFLOW §7.5 option (a) (build/pgTest on S + Codex review) for each, not revert.
- Open (2026-11-28): deadline risk. Tasks/Availability/Fixed Events (#35/#36/#39) not merged 5 days before 2026-12-03; scheduling needs them. Merge order (PR #48 body): #48 → #36 → #39 → #35. R48-1 (uncapped edit/confirm expansion) must be fixed before #36 merges. Scope cuts not yet proposed.
- Open: merges by `kiet232k6` pass `REVIEW_REQUIRED` through the ruleset admin bypass (5 times in a row, #43–#47; each state-recording PR created a new exception). Root-cause options for TV1: (i) mentor submits an Approve review on GitHub at the PR head and TV1 merges; (ii) TV1 narrows the ruleset bypass actor; (iii) record a team decision via a WORKFLOW docs PR. Avoid opening PRs only to record these exceptions.
- Open: skill `schedule` and `Agent` with `isolation: "remote"` cannot be denied by tool name.
- Open: non-iCloud clone has no `/.claude/settings.local.json` entry in `.git/info/exclude`; add it before creating that file.
- Open: read-only `gh` token expires 2026-12-18.
- Open: merged branches `docs/project-state-and-review-skills`, `docs/project-state-pr43`, `docs/project-state-pr44` and `docs/project-state-pr45` not yet deleted (local/remote).

## Resume

- Completed: GOV-01, GOV-02 (verified); GOV-03, GOV-04 (merged in fcf7919); GOV-05 (applied outside repo)
- Pending: PR #48 (API-01) review/accept; R48-1 follow-up; author fixes on #35/#36/#39; root-cause decision on the admin bypass; GOV-08 to GOV-12 Codex reviews + §7.5 records; A4/A5 reruns; token renewal before 2026-12-18; delete merged branches (incl. `docs/project-state-pr46`); update local `develop`
- Next action: TV1 posts the #48 review, gets TV2 + TV4 reviews and runs Codex; TV1 decides the R48-1 rule (horizon vs span cap). Governance items after.

Update after each completed task. Revalidate stale information when relevant code changes. Never mark unverified work as VERIFIED. Avoid duplicate analysis.
