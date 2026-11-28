# Project State

Persistent development state. Keep concise, evidence-based, and up to date.

## Context

- Project: PlanWise AI (task management + smart scheduling web app)
- Stack: Spring Boot backend (Java 25, Spring Boot 4.1.x, Gradle 9.8, PostgreSQL/H2); React frontend
- Architecture: See CLAUDE.md, Code/backend/CLAUDE.md, Code/frontend/CLAUDE.md
- Branch: docs/project-state-and-review-skills (pushed, upstream set; PR not opened)
- Last reviewed commit: b215cf1
- Current milestone: Not reviewed in this session (module progress not re-verified)
- Updated: 2026-11-28

## Progress

| ID  | Feature / Task | Status | Evidence | Dependencies |
| --- | -------------- | ------ | -------- | ------------ |
| GOV-01 | Governance preflight (WORKFLOW §9), read-only | VERIFIED | Session 2026-11-28: hooks, branch, `gh` preflight 200 as `kietit2k6` | — |
| GOV-02 | Move work off iCloud-synced clone to a non-iCloud clone | VERIFIED | Branch fetched from old clone, HEAD b09c5e0 matched; 6 iCloud duplicate files were byte-identical to tracked originals and deleted from old clone | — |
| GOV-03 | P-RO policy: deny external-write tools and bare `git -C * push` | IMPLEMENTED | d654659; `.claude/settings.json` sha256 `5b26a96a3cafbfccf70c29dbca926ec8e8bf6632f267ce0e72fbfcba1a0c4f34` | A4 rerun |
| GOV-04 | Update pr-review skill | IMPLEMENTED | b215cf1 (blob 19b414a) | PR review |
| GOV-05 | Codex config: `sandbox_mode = "read-only"`, `approval_policy = "never"`, project path updated (outside repo) | IMPLEMENTED | Config parsed OK; behavior not tested | A5 rerun |
| GOV-06 | Fixture A4 rerun with new settings hash | BLOCKED | Needs TV1, new session in the non-iCloud clone | GOV-03 |
| GOV-07 | Fixture A5 rerun (Codex) | BLOCKED | Needs TV1 | GOV-05 |

Status: TODO / IN_PROGRESS / IMPLEMENTED / VERIFIED / BLOCKED / DEFERRED.

## Quality

- Build: NOT_RUN (docs/config-only changes)
- Lint: NOT_RUN
- Type check: NOT_RUN
- Tests: NOT_RUN
- Security audit: See docs/security/AUDIT.md

## Current Task

- ID: None (governance maintenance, no PW Issue)
- Objective: Governance preflight, clone migration, P-RO policy hardening
- Status: IMPLEMENTED (pushed at b215cf1; PR to `develop` not opened)
- Acceptance criteria: PR merged per WORKFLOW §7.1; A4 and A5 pass

## Next Task

- ID: GOV-06 / GOV-07
- Priority: High
- Objective: Open PR `docs/project-state-and-review-skills` -> `develop`; rerun A4 and A5
- Reason: Policy changed (WORKFLOW §9 requires A4 rerun); Codex config changed
- Dependencies: TV1 opens the PR; new Claude session in the non-iCloud clone
- Acceptance criteria: A4 hash matches record; A5 cases pass (no INCONCLUSIVE)
- Approval: PENDING

## Blockers & Decisions

- 2026-11-28, TV1: auto permission mode accepted for the 2026-11-28 session only. WORKFLOW §9 still says `default`; no docs PR yet.
- 2026-11-28, TV1: risk accepted: `git push` credentials remain in `osxkeychain` (HTTPS remote, no SSH keys). Deny-list bypasses (env/sh -c/alias/script) are not blockable by settings.
- Open: skill `schedule` and `Agent` with `isolation: "remote"` cannot be denied by tool name.
- Open: non-iCloud clone has no `/.claude/settings.local.json` entry in `.git/info/exclude`; add it before creating that file.
- Open: read-only `gh` token expires 2026-12-18.

## Resume

- Completed: GOV-01, GOV-02 (verified); GOV-03, GOV-04, GOV-05 (implemented, pushed/applied)
- Pending: PR to `develop`; A4/A5 reruns; token renewal before 2026-12-18
- Next action: TV1 opens PR, then starts a new session in the non-iCloud clone and reruns A4.

Update after each completed task. Revalidate stale information when relevant code changes. Never mark unverified work as VERIFIED. Avoid duplicate analysis.
