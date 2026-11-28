# Project State

Persistent development state. Keep concise, evidence-based, and up to date.

## Context

- Project: PlanWise AI (task management + smart scheduling web app)
- Stack: Spring Boot backend (Java 25, Spring Boot 4.1.x, Gradle 9.8, PostgreSQL/H2); React frontend
- Architecture: See CLAUDE.md, Code/backend/CLAUDE.md, Code/frontend/CLAUDE.md
- Branch: develop (PR #45 squash-merged as 56d4364)
- Last reviewed commit: 56d4364 (tree 9744090, same as PR #45 head 424a27d)
- Current milestone: Not reviewed in this session (module progress not re-verified)
- Updated: 2026-11-28

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

Status: TODO / IN_PROGRESS / IMPLEMENTED / VERIFIED / BLOCKED / DEFERRED.

## Quality

Checked on tree 9744090 (= 56d4364), 2026-11-28 09:37 +07, `./gradlew --offline --rerun-tasks build pgTest` (9/9 tasks executed):

- Build: PASS
- Lint: NOT_RUN locally (CI `frontend` job passed on 424a27d)
- Type check: NOT_RUN locally (CI `frontend` job passed on 424a27d)
- Tests: PASS (`test` H2: 364 tests, 0 failures, 0 errors, 0 skipped; `pgTest` PostgreSQL 16: 167 tests, 0 failures, 0 errors, 0 skipped)
- CI on 424a27d: frontend, gitflow, pg, test all pass
- Security audit: See docs/security/AUDIT.md

## Current Task

- ID: GOV-08 / GOV-09 / GOV-10 (governance maintenance, no PW Issue)
- Objective: Close the PR #43, #44 and #45 post-merge exceptions per WORKFLOW §7.5 option (a)
- Status: IN_PROGRESS
- Acceptance criteria: Codex review of `git diff M S` done for each PR (expected empty); §7.5 record posted on PR #43, #44 and #45 by TV1

## Next Task

- ID: GOV-06 / GOV-07
- Priority: High
- Objective: Rerun A4 (new settings hash) and A5 (Codex)
- Reason: Policy changed (WORKFLOW §9 requires A4 rerun); Codex config changed
- Dependencies: GOV-08, GOV-09, GOV-10; new Claude session in the non-iCloud clone
- Acceptance criteria: A4 hash matches record; A5 cases pass (no INCONCLUSIVE)
- Approval: PENDING

## Blockers & Decisions

- 2026-11-28, TV1: auto permission mode accepted for the 2026-11-28 session only. WORKFLOW §9 still says `default`; no docs PR yet.
- 2026-11-28, TV1: risk accepted: `git push` credentials remain in `osxkeychain` (HTTPS remote, no SSH keys). Deny-list bypasses (env/sh -c/alias/script) are not blockable by settings.
- 2026-11-28, TV1: PR #43, #44 and #45 merged without peer review or D12 waiver; TV1 chose WORKFLOW §7.5 option (a) (build/pgTest on S + Codex review) for each, not revert.
- Open: merges by `kiet232k6` pass `REVIEW_REQUIRED` through the ruleset admin bypass (3 times in a row, #43–#45). Root-cause options for TV1: (i) mentor submits an Approve review on GitHub at the PR head and TV1 merges; (ii) TV1 narrows the ruleset bypass actor; (iii) record a team decision via a WORKFLOW docs PR. Avoid opening PRs only to record these exceptions.
- Open: skill `schedule` and `Agent` with `isolation: "remote"` cannot be denied by tool name.
- Open: non-iCloud clone has no `/.claude/settings.local.json` entry in `.git/info/exclude`; add it before creating that file.
- Open: read-only `gh` token expires 2026-12-18.
- Open: merged branches `docs/project-state-and-review-skills`, `docs/project-state-pr43` and `docs/project-state-pr44` not yet deleted (local/remote).

## Resume

- Completed: GOV-01, GOV-02 (verified); GOV-03, GOV-04 (merged in fcf7919); GOV-05 (applied outside repo)
- Pending: GOV-08/GOV-09/GOV-10 Codex reviews + §7.5 records; root-cause decision on the admin bypass; A4/A5 reruns; token renewal before 2026-12-18; delete merged branches
- Next action: TV1 runs Codex review of `git diff M S` for #43, #44 and #45 in the non-iCloud clone, posts the three §7.5 records, then reruns A4 in a new session.

Update after each completed task. Revalidate stale information when relevant code changes. Never mark unverified work as VERIFIED. Avoid duplicate analysis.
