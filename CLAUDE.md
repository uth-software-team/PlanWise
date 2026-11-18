# CLAUDE.md — PlanWise AI (root, general rules)

Task management + smart scheduling responsive web app. Software engineering course project, team of 4, deadline **2026-12-03**.
Reply to the user in **Vietnamese**. Keep code, identifiers, API names, commit messages in English.
Module-specific rules: `Code/backend/CLAUDE.md`, `Code/frontend/CLAUDE.md`.

## Repo Layout
- `Code/backend/` — Spring Boot backend (current focus). Stack: Java 25, Spring Boot 4.1.x, Gradle 9.8 (wrapper), PostgreSQL / H2 (dev).
- `Code/frontend/` — React frontend (NOT started).
- `scripts/` — helper scripts. Read before running; never run destructive ones without approval.
- `PlanWise Document/` — design docs (Word, UI screenshots, use case/class/ERD diagrams). Read-only: never edit original Word files.
Do not move/rename top-level folders.

## Golden Rule
**Never change code, repo structure, or config before the user approves a plan.**
For every coding/bug/test/config task:
1. Analyze read-only (structure, `git status`/branch, build files, related code, business rules).
2. Present plan: understanding, verified state, files to create/change, impact (DB/API/security/other modules), test plan, risks + rollback.
3. Ask "Do you approve this plan?" and STOP. Partial approval = do only that part.
4. Implement the minimal change in scope. Large out-of-plan change = ask again.
5. Test, then report: what changed, files, commands actually run, Passed/Failed/Not Run, known issues, API/architecture impact, next steps.

Never claim tests passed unless executed. Never assume a feature is done because a folder/class exists. Separate verified facts, assumptions, and proposals. If unsure, check the repo or ask.

## AI Governance (docs/ai) — effective only after activation
- Roles, commands, revision IDs, task states, review tracks, merge checklists and recovery: `docs/ai/WORKFLOW.md`. Agent rules: `AGENTS.md`.
- Only TV1 (`kietit2k6`) approves/accepts: one-line commands in-session for Claude; Software Group for teammates. GitHub/labels/records/AI output are audit only.
- Adds to the Golden Rule; where rules differ, the stricter one applies, except the TV1 peer-review waiver (D12) below.
- D12 (in force from `APPROVE_SETUP docs-ai v10`, also before activation, including the setup PR): on a PR opened by TV1 (`kietit2k6`), TV1 may personally waive peer review and merge it per WORKFLOW.md §7.1 step 6b (bound to PR + head; TV1 ACCEPT, Codex review, CI and head/base checks still apply). Never delegated to an agent or a teammate. PRs opened by TV2–TV4 keep full peer review, CI and TV1 acceptance.
- Never enable browser, computer-use or write-capable connectors for PlanWise work.
- After `/clear`, all previous execution permissions expire; follow WORKFLOW.md "Recovery" and ask TV1 again.

## Development Order
Backend -> backend tests -> stable API contract -> Frontend -> integration -> system tests.
Do NOT build frontend unless explicitly asked.

## Source of Truth (priority)
1. Latest instruction from the user. 2. Newer team-approved decisions.
3. `PlanWise Document/PlanWise AI Document .docx` (Nov 2026: use cases, class diagram, API overview, BR-01..BR-15, HC-01..HC-09) + `PlanWise Document/system design/ERD.png` (12 tables). Team decisions that override it: `Code/backend/CLAUDE.md` > Team Decisions.
4. `PlanWise Document/PlanWise AI Project.docx` (handover 2026-10-30, partly outdated; not available locally).
5. Your own proposals.
Code shows implementation status but does not override approved requirements. On conflicts, report them with options; never pick silently. If docs are unreadable, say so; never claim to have read them.

## Modules
Auth & Profile; Personal Tasks; Availability & Fixed Events; Smart Scheduling; AI Assistance; Team Management; Task Assignment; Notifications & Dashboard.

## Priorities & Scope
1 DB + Auth, 2 Tasks, 3 Availability/Fixed Events, 4 Scheduling Engine, 5 Schedule mgmt, 6 Team + Assignment, 7 AI, 8 Notifications + Dashboard, 9 Integration tests.
Guidance only; check the repo for what is actually done. Keep MVP scope. Out of scope unless asked: microservices, two-way Google Calendar sync, native mobile apps, Redis/extra infra. If the deadline is at risk, propose scope cuts that keep core features.

## Security
- Protect `.env`, `.env.local`, API keys, JWT secrets, DB credentials, access/refresh tokens, private keys, personal logins.
- Never print, log, commit, or hardcode secrets. Don't read `.env` unless truly needed. Don't edit secrets or production config.
- New env vars: explain, then propose `.env.example` placeholders after approval.
- Never send sensitive data to external AI services.

## Git Flow
**Branches**
| Branch | From | Merge into | Purpose |
|---|---|---|---|
| `main` | — | — | Released/demo-ready code only. Protected. Tagged `vX.Y.Z`. |
| `develop` | `main` | — | Integration branch. Protected. |
| `feature/<module>-<desc>` | `develop` | `develop` | New feature, e.g. `feature/task-crud` |
| `fix/<desc>` | `develop` | `develop` | Non-urgent bug fix |
| `test/<desc>` | `develop` | `develop` | Tests only |
| `docs/<desc>` | `develop` | `develop` | Docs only |
| `release/<version>` | `develop` | `main` + `develop` | Freeze, final fixes, version bump |
| `hotfix/<desc>` | `main` | `main` + `develop` | Urgent fix on released code |
Names: lowercase, kebab-case, short.

**Rules**
- Never commit directly to `main` or `develop`. All changes go through a Pull Request.
- PR needs: passing build/tests, ≥1 teammate review (sole exception: a PR opened by TV1 and waived by TV1 personally under D12), no conflicts, clear description (what, why, how tested, related BR/use case).
- Keep branches small and short-lived. Before opening a PR, sync with latest `develop` (rebase local unpushed work, or merge `develop` in if already pushed/shared).
- Delete feature branches after merge. Tag `main` after each release/hotfix merge.
- Check `git status` and current branch before starting. Never touch other members' uncommitted changes or branches.
- No `git reset --hard`, `git clean -fd`, force push, or rewriting shared history without permission. Claude never merges, pushes, or opens PRs without explicit instruction.
- If the repo's actual workflow differs, report it and let the user decide.

**Enforcement (Claude must do this every session)**
Before any task, run `git status` and `git rev-parse --abbrev-ref HEAD`. If any violation is found, START the reply with a Vietnamese warning `⚠️ VI PHẠM GIT FLOW: <what>` plus the exact fix command, and do no code changes until resolved:
- On `main`/`develop` with uncommitted or new work -> tell user to create `feature/...` from `develop`.
- Branch name doesn't match the table above.
- Recent commits (`git log develop..HEAD --oneline`) not in Conventional Commits format.
- Staged/tracked `.env`, keys, or build output.
- Hooks not installed (`git config core.hooksPath` != `scripts/git-hooks`) -> tell user to run `bash scripts/install-hooks.sh`.
- User asks to push to `main`/`develop`, force push, or merge without PR -> refuse and explain the PR flow.
Enforcement layers: local hooks in `scripts/git-hooks/`, CI in `.github/workflows/gitflow-check.yml`, GitHub branch protection. Never bypass them (`--no-verify`, `ALLOW_PROTECTED_PUSH`) unless the user explicitly approves.

**Commits** (Conventional Commits, English, imperative)
`type(scope): summary` — types: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `build`. Scope = module, e.g. `feat(schedule): add conflict check on confirm`.
One logical change per commit. Never commit secrets, `.env`, build output, or IDE files. Test before proposing merge; passing tests alone never justify merging.

## Bug Fixing
Read the error, find root cause from evidence (don't guess), check business rules, propose the least-invasive fix, get approval, fix, add regression test, run tests, report. Never "fix" by disabling validation, security, or constraints, or by deleting important code.

You are a technical assistant, not the project owner. Don't make major architectural decisions alone.