# Project State

Persistent development state. Keep concise, evidence-based, and up to date.

## Context

- Project: PlanWise AI (task management + smart scheduling web app)
- Stack: Spring Boot backend (Java 25, Spring Boot 4.1.x, Gradle 9.8, PostgreSQL/H2); React frontend
- Architecture: See CLAUDE.md, Code/backend/CLAUDE.md, Code/frontend/CLAUDE.md
- Branch: develop (PR #43 squash-merged as fcf7919)
- Last reviewed commit: fcf7919 (tree a111c56, same as PR head 94a4a06)
- Current milestone: Not reviewed in this session (module progress not re-verified)
- Updated: 2026-11-28

## Progress

| ID  | Feature / Task | Status | Evidence | Dependencies |
| --- | -------------- | ------ | -------- | ------------ |
| GOV-01 | Governance preflight (WORKFLOW §9), read-only | VERIFIED | Session 2026-11-28: hooks, branch, `gh` preflight 200 as `kietit2k6` | — |
| GOV-02 | Move work off iCloud-synced clone to a non-iCloud clone | VERIFIED | Branch fetched from old clone, HEAD b09c5e0 matched; 6 iCloud duplicate files were byte-identical to tracked originals and deleted from old clone | — |
| GOV-03 | P-RO policy: deny external-write tools and bare `git -C * push` | IMPLEMENTED | Merged in fcf7919 (PR #43); `.claude/settings.json` sha256 `5b26a96a3cafbfccf70c29dbca926ec8e8bf6632f267ce0e72fbfcba1a0c4f34`; policy behavior not yet tested | GOV-06 |
| GOV-04 | Update pr-review skill | IMPLEMENTED | Merged in fcf7919 (PR #43) | — |
| GOV-05 | Codex config: `sandbox_mode = "read-only"`, `approval_policy = "never"`, project path updated (outside repo) | IMPLEMENTED | Config parsed OK; behavior not tested | GOV-07 |
| GOV-06 | Fixture A4 rerun with new settings hash | BLOCKED | Needs TV1, new session in the non-iCloud clone | GOV-03 |
| GOV-07 | Fixture A5 rerun (Codex) | BLOCKED | Needs TV1 | GOV-05 |
| GOV-08 | PR #43 post-merge exception (WORKFLOW §7.5, option a) | IN_PROGRESS | Merged by `kiet232k6` with 0 reviews, no `WAIVE_PEER_REVIEW`. S=fcf7919 has 1 parent == D=6d59f3a; tree(S) == tree(H) == a111c56; tree(M) inferred equal (D ancestor of H, CI log not checked). Local build/pgTest on tree(S) passed | Codex review `git diff M S` (TV1) |

Status: TODO / IN_PROGRESS / IMPLEMENTED / VERIFIED / BLOCKED / DEFERRED.

## Quality

Checked on tree a111c56 (= fcf7919), 2026-11-28 09:18 +07, `./gradlew --offline build pgTest`:

- Build: PASS
- Lint: NOT_RUN locally (CI `frontend` job passed on 94a4a06)
- Type check: NOT_RUN locally (CI `frontend` job passed on 94a4a06)
- Tests: PASS (`test` H2: 364 tests, 0 failures, 0 errors, 0 skipped; `pgTest` PostgreSQL 16: 167 tests, 0 failures, 0 errors, 0 skipped)
- CI on 94a4a06: frontend, gitflow, pg, test all pass
- Security audit: See docs/security/AUDIT.md

## Current Task

- ID: GOV-08 (governance maintenance, no PW Issue)
- Objective: Close the PR #43 post-merge exception per WORKFLOW §7.5 option (a)
- Status: IN_PROGRESS
- Acceptance criteria: Codex review of `git diff M S` done (expected empty); §7.5 record posted on PR #43 by TV1

## Next Task

- ID: GOV-06 / GOV-07
- Priority: High
- Objective: Rerun A4 (new settings hash) and A5 (Codex)
- Reason: Policy changed (WORKFLOW §9 requires A4 rerun); Codex config changed
- Dependencies: GOV-08; new Claude session in the non-iCloud clone
- Acceptance criteria: A4 hash matches record; A5 cases pass (no INCONCLUSIVE)
- Approval: PENDING

## Blockers & Decisions

- 2026-11-28, TV1: auto permission mode accepted for the 2026-11-28 session only. WORKFLOW §9 still says `default`; no docs PR yet.
- 2026-11-28, TV1: risk accepted: `git push` credentials remain in `osxkeychain` (HTTPS remote, no SSH keys). Deny-list bypasses (env/sh -c/alias/script) are not blockable by settings.
- 2026-11-28, TV1: PR #43 merged without peer review or D12 waiver; TV1 chose WORKFLOW §7.5 option (a) (build/pgTest on S + Codex review), not revert.
- Open: skill `schedule` and `Agent` with `isolation: "remote"` cannot be denied by tool name.
- Open: non-iCloud clone has no `/.claude/settings.local.json` entry in `.git/info/exclude`; add it before creating that file.
- Open: read-only `gh` token expires 2026-12-18.
- Open: merged branch `docs/project-state-and-review-skills` not yet deleted.

## Resume

- Completed: GOV-01, GOV-02 (verified); GOV-03, GOV-04 (merged in fcf7919); GOV-05 (applied outside repo)
- Pending: GOV-08 Codex review + §7.5 record; A4/A5 reruns; token renewal before 2026-12-18; delete merged branch
- Next action: TV1 runs Codex review of `git diff M S`, posts the §7.5 record on PR #43, then reruns A4 in a new session.

Update after each completed task. Revalidate stale information when relevant code changes. Never mark unverified work as VERIFIED. Avoid duplicate analysis.
