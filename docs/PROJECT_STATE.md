# Project State

Persistent development state. Keep concise, evidence-based, and up to date.

## Context

- Project: PlanWise AI (task management + smart scheduling web app)
- Stack: Spring Boot backend (Java 25, Spring Boot 4.1.x, Gradle 9.8, PostgreSQL/H2); React frontend
- Architecture: See CLAUDE.md, Code/backend/CLAUDE.md, Code/frontend/CLAUDE.md
- Branch: `develop` = origin/develop 1a91375 (#49 0df1ec9, #50 4b44492, #51 1a91375 merged, all without review: GOV-14/15/16). Working branch: `feature/fe-auth` (FE-AU, not committed)
- Last reviewed commit: 0df1ec9 (squash tree == #49 head 43f8500); #49/#50/#51 diffs reviewed by Claude (docs/reviews/PR_REVIEW.md)
- Current milestone: Priorities 2–3 (Tasks, Availability/Fixed Events) are in open PRs #35/#36/#39, not merged. Deadline 2026-12-03 (5 days)
- Updated: 2026-11-28 (#50/#51 merged → GOV-15/16 recorded; FE-AU implemented on `feature/fe-auth`, not committed)

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
| GOV-13 | PR #48 post-merge exception (WORKFLOW §7.5) | TODO | Merged by `kiet232k6` as 5d03acb (2026-11-28 03:54Z), head 8e7db4b (not 7323c47), `reviewDecision` REVIEW_REQUIRED; only review = COMMENTED by kietit2k6 on old head 7323c47; no `ACCEPT`/`WAIVE_PEER_REVIEW` comment (6th in a row). S=5d03acb has 1 parent == D=f6f602d; tree(S) == tree(H=8e7db4b) == f1e6b19; CI 4/4 SUCCESS on 8e7db4b. Codex review + §7.5 record still owed Tree compare of the squash commit vs PR head does not complete the §7.5 `git diff M S` review | Admin-bypass root-cause decision (TV1) |
| GOV-14 | PR #49 post-merge exception (WORKFLOW §7.5, option a) | IN_PROGRESS | Merged by `kietit2k6` as 0df1ec9 (2026-11-28 09:35:56Z), `reviewDecision` REVIEW_REQUIRED, 0 reviews, 0 comments, no `ACCEPT`/`WAIVE_PEER_REVIEW`, no recorded Codex recheck at head 43f8500 (7th admin bypass in a row). S=0df1ec9 has 1 parent == D=5d03acb; tree(S) == tree(H=43f8500) == af64b27. Build/pgTest: H2 375/375 + pg 169/169 on code == 24ff666 == tree(S) code (09:18Z); also local `./gradlew --offline --rerun-tasks build pgTest` (2026-11-28 16:38 +07, 9/9 tasks) on the #50 sync tree (origin/develop 0df1ec9 merged into 7473621, not committed): XML `test` 26 files, 379 tests (09:38:48–09:38:58Z); `pgTest` 7 files, 169 tests (09:38:59–09:39:06Z); 0 failures/errors/skipped (contains 0df1ec9). No run on a clean checkout of tree(S) itself | Codex review `git diff M S` (TV1); §7.5 record (TV1) |
| GOV-15 | PR #50 post-merge exception (WORKFLOW §7.5, option a) | IN_PROGRESS | Merged by `kietit2k6` as S=4b44492 (2026-11-28 09:50:09Z), `reviewDecision` REVIEW_REQUIRED, 0 reviews, 0 comments, no `ACCEPT`/`WAIVE_PEER_REVIEW` (8th admin bypass in a row). D=0df1ec9 (S^, 1 parent); H=f40acbd. CI run 46131614046 attempt 1 (`pull_request`, head f40acbd): frontend/pg/gitflow/test SUCCESS; all 4 jobs checked out M=1eff662 (`refs/pull/50/merge`, parents [0df1ec9, f40acbd]); tree(M) == tree(S) == tree(H) == 68440f6 (M read via `gh api` commit object). Local on a clean `git archive 4b44492` (2026-11-28): `./gradlew --offline --rerun-tasks build pgTest` 9/9 tasks, XML `test` 26 files 379 tests (10:07:52–10:08:03Z), `pgTest` 7 files 169 tests (10:08:04–10:08:10Z), 0 failures/errors/skipped | TV1 chose §7.5 option (a) (2026-11-28); build/pgTest on S part done (clean `git archive 4b44492`, see Evidence). Remaining: Codex review `git diff 1eff662 4b44492` (TV1); §7.5 record (TV1) |
| GOV-16 | PR #51 post-merge exception (WORKFLOW §7.5, option a) | IN_PROGRESS | Merged by `kietit2k6` as S=1a91375 (2026-11-28 09:50:27Z), `reviewDecision` REVIEW_REQUIRED, 0 reviews, no `ACCEPT`/`WAIVE_PEER_REVIEW` (9th admin bypass in a row). D=4b44492 (base at squash; S^, 1 parent); H=2887f76. CI run 46108729199 attempt 1 (`pull_request`, 04:41Z, head 2887f76): test/pg/frontend/gitflow SUCCESS, but on base 5d03acb: M_CI=6a5a28e (parents [5d03acb, 2887f76]), tree 9be6235 == tree(H) != tree(S) a3e531f. No CI run on base 4b44492; GitHub's merge ref at squash time not recorded. Local `git merge-tree 4b44492 2887f76` = a3e531f == tree(S) (computed locally, not a GitHub M). `Code/frontend` and `.github` equal between 5d03acb and 4b44492, and between H and S. Local on a clean `git archive 1a91375` (2026-11-28): build/pgTest 9/9 tasks, `test` 379 tests (10:08:14–10:08:24Z), `pgTest` 169 tests (10:08:25–10:08:31Z), 0 failures/errors/skipped; frontend `npm ci`, lint, format:check, build, check:api-client 10/10 all exit 0 | TV1 chose (i) (2026-11-28): M = tree a3e531f from `git merge-tree --write-tree 4b44492 2887f76` (local, not a GitHub ref); `git diff a3e531f 1a91375` is empty (checked 2026-11-28). TV1 chose §7.5 option (a) (2026-11-28); build/pgTest + frontend checks on S part done (clean `git archive 1a91375`, see Evidence). Remaining: Codex review of that diff (TV1); §7.5 record (TV1) |
| API-01 | Max query range 366 days for group availability + schedule generate (PR #48, TV1) | IMPLEMENTED | Merged in 5d03acb (head 8e7db4b): `DateUtils.exceedsMaxQueryRange`, checks in `GroupAvailabilityServiceImpl` and `ScheduleService.generateProposal`, edit-session batch span cap, OpenAPI + `schema.d.ts`. R48-1 (edit-session span cap, D14) and R48-2 (D14/D15 rename) merged here. No build/pgTest on 5d03acb itself; its child 827b9f0 passed (see SC-FIX-01). Merged without review (GOV-13) | GOV-13 |
| SC-FIX-01 | Scope remaining-minutes query and lock batch before editing sessions (PR #49, TV1) | IMPLEMENTED | Merged as 0df1ec9 (squash of 827b9f0 fix, 398aa49 + 24ff666 tests, docs commits; tree == head 43f8500). CI 4/4 on 43f8500 (run 46130033955). Local on code == 24ff666: H2 375/375, pg 169/169, both SA-17 case names in the XML. Claude review COMMENT at 85ecb02; R49-1/R49-2 then fixed by Claude itself (not independent). Codex REQUEST_CHANGES at 85ecb02 (history); no recorded recheck before merge. Merged without review (GOV-14) | GOV-14 |
| FE-AU | Login / Register / Profile (Settings) + UserMenu, TV1 (`feature/fe-auth` from origin/develop 1a91375) | IMPLEMENTED | Not committed (2026-11-28). `features/auth` (LoginPage, RegisterPage, SettingsPage, session.ts, authApi.ts), `lib/safe-redirect.ts` (SA-13), `ApiError.fieldErrors`, PublicOnlyRoute, router, UserMenu, shadcn input/label. Local: lint, format:check, `tsc -b`, build OK (bundle 500.40 kB > 500 kB warning, was 483 kB); check:api-client 19/19 (T1–T13); smoke through Vite proxy on isolated H2 (`fe_au_smoke`, Flyway off, create-drop: migration V5 fails on H2) 14 cases as expected + I2–I3 2/2. Browser UI checklist NOT_RUN (TV1). Settings limited to Profile; no "Quên mật khẩu?" (no backend endpoint); dev token bar kept (SA-11) | TV1 UI checklist; review (TV3/TV4 or D12), Codex, ACCEPT |
| TK-01 | Personal task CRUD + TaskApi (PR #35, Khoi12vn) | IN_PROGRESS | CI 4/4 SUCCESS. TV1 review 2026-11-28 03:02Z: [MEDIUM] group tasks leak into personal endpoints. TV1 already decided: D15 (Code/backend/CLAUDE.md) = personal only, group task id 404. CHANGES_REQUESTED 03:43Z; head a5da7cc unchanged since review; 7 commits behind `develop` | Author fix |
| AV-02 | Availability rule CRUD + interval expansion (PR #36, Khoi12vn) | IN_PROGRESS | CI 4/4 SUCCESS. TV1 review: [HIGH] empty `fixedEventOccurrences` lets scheduling ignore fixed events (merge #39 right after, or throw NOT_IMPLEMENTED); [LOW] per-day loop. CHANGES_REQUESTED 03:42Z; head dbb7d1b unchanged since review; 12 commits behind `develop` | Author fix; merge order with #39 |
| AV-03 | Fixed events + calendar (PR #39, Khoi12vn) | IN_PROGRESS | CI 4/4 SUCCESS. TV1 review: [MEDIUM] WEEKLY expansion untested (4 cases); [MEDIUM] unbounded calendar range (to use API-01 helper, now on `develop`). CHANGES_REQUESTED 03:41Z; head 6a090b2 unchanged since review; 12 commits behind `develop` | Author fix |
| SEC-01 | Security audit fixes: SA-02 (placeholder JWT secret) + SA-04 (JWT issuer) in PR #50; SA-10 (index.html) + SA-11 (dev-only token bar) in PR #51 | IMPLEMENTED | 2026-11-28: backend 4faab9f — H2 379/379, pgTest 167/167 (`--rerun-tasks`); frontend 2887f76 — lint/tsc/build OK, check:api-client 10/10, prod bundle has no token bar. PR #50 head 9ae6441 (f222868 + AUDIT.md SA-20 record; CI in progress 2026-11-28) and #51 head 2887f76 (CI 4/4 SUCCESS); 0 reviews. #50 sync with develop 0df1ec9 in progress (R50-1 resolved, R50-4 docs fix) Codex recommendation (source: khuyến nghị kỹ thuật của Codex tại SHA đã kiểm tra): backend at f222868 COMMENT, no blocking issue, R50-3 LOW added (setup-dev.sh sed breaks on `/` in Base64); frontend at 2887f76 no finding (tsc/ESLint, no prod build). SA-01/03/09/12 deferred (TV1). Details: docs/security/AUDIT.md (on #50) | Review, TV1 ACCEPT |

Status: TODO / IN_PROGRESS / IMPLEMENTED / VERIFIED / BLOCKED / DEFERRED.

## Quality

Checked on the #50 sync tree (origin/develop 0df1ec9 merged into `fix/auth-jwt-hardening` 7473621, not committed), 2026-11-28 16:38 +07, `./gradlew --offline --rerun-tasks build pgTest` (9/9 tasks executed); counts read from the XML on disk:

- Build: PASS
- Lint: NOT_RUN (backend only)
- Type check: NOT_RUN (backend only)
- Tests: PASS (`test` H2: 379 tests, 0 failures, 0 errors, 0 skipped; `pgTest` PostgreSQL 16: 169 tests, 0 failures, 0 errors, 0 skipped; XML 26 + 7 files, 09:38:48–09:39:06Z). 379 = 375 on develop + 4 JWT tests of #50
- CI: 4/4 SUCCESS on #49 43f8500 (run 46130033955, merged), #50 7473621 (run 46130160284; sync merge not pushed yet), #51 2887f76
- Security audit: See docs/security/AUDIT.md

## Current Task

- ID: GOV-08 / GOV-09 / GOV-10 / GOV-11 (governance maintenance, no PW Issue)
- Objective: Close the PR #43–#46 post-merge exceptions per WORKFLOW §7.5 option (a)
- Status: IN_PROGRESS
- Acceptance criteria: Codex review of `git diff M S` done for each PR (expected empty); §7.5 record posted on PR #43–#46 by TV1

## Next Task

- ID: REV-49-51 (Claude read-only `/pr-review` of #49 → #50 → #51); #49 merged (GOV-14)
- Priority: Highest (deadline 2026-12-03; product work outranks GOV-06/07)
- Objective: Review the three TV1 fix PRs so they can merge properly
- Reason: Only unblocked product items; merge-ready except review; no diff reviewed yet. #35/#36/#39 are blocked on the author
- Dependencies: TV1 approval to start; then peer review or D12 waiver; Codex review
- Acceptance criteria: one review per PR in docs/reviews/PR_REVIEW.md, no unresolved HIGH/MEDIUM; head/base checks per WORKFLOW §7.1; TV1 ACCEPT; merged without admin bypass (no GOV-14); post-merge build/pgTest passes
- In parallel (TV1): ping Khoi12vn with a deadline for #36 → #39 → #35 fixes; if none by 2026-11-30, TV1 decides scope cuts or takeover
- After: GOV-06/07 (A4/A5 reruns) once GOV-08..13 close
- Approval: APPROVED (TV1 2026-11-28), in progress

## Blockers & Decisions

- 2026-11-28, TV1: auto permission mode accepted for the 2026-11-28 session only. WORKFLOW §9 still says `default`; no docs PR yet.
- 2026-11-28, TV1: risk accepted: `git push` credentials remain in `osxkeychain` (HTTPS remote, no SSH keys). Deny-list bypasses (env/sh -c/alias/script) are not blockable by settings.
- 2026-11-28, TV1: PR #43, #44, #45 and #46 merged without peer review or D12 waiver; TV1 chose WORKFLOW §7.5 option (a) (build/pgTest on S + Codex review) for each, not revert.
- 2026-11-28, TV1: GOV-15 (#50) §7.5 option (a) (build/pgTest on S + Codex review `git diff M S`), not revert. Build/pgTest on a clean export of S=4b44492 already passed (379 H2 + 169 pg).
- 2026-11-28, TV1: GOV-16 (#51) M choice (i): CI never ran on the squash base 4b44492 and GitHub's merge ref was not recorded, so M = the locally computed merge tree a3e531f (`git merge-tree --write-tree 4b44492 2887f76`), recorded as local, not a GitHub M.
- 2026-11-28, TV1: GOV-16 (#51) §7.5 option (a), not revert. Build/pgTest (379 H2 + 169 pg) and frontend checks (lint, format:check, build, check:api-client 10/10) on a clean export of S=1a91375 already passed.
- Open (2026-11-28): deadline risk. Tasks/Availability/Fixed Events (#35/#36/#39) not merged 5 days before 2026-12-03; scheduling needs them. Merge order: #36 → #39 → #35 (#48 merged). R48-1 fixed in 5d03acb. Scope cuts not yet proposed.
- Open: merges by `kiet232k6` pass `REVIEW_REQUIRED` through the ruleset admin bypass (9 times in a row, #43–#51; #49–#51 merged by `kietit2k6`; each state-recording PR created a new exception). Root-cause options for TV1: (i) mentor submits an Approve review on GitHub at the PR head and TV1 merges; (ii) TV1 narrows the ruleset bypass actor; (iii) record a team decision via a WORKFLOW docs PR. Avoid opening PRs only to record these exceptions.
- Open: skill `schedule` and `Agent` with `isolation: "remote"` cannot be denied by tool name.
- Open: non-iCloud clone has no `/.claude/settings.local.json` entry in `.git/info/exclude`; add it before creating that file.
- Open: read-only `gh` token expires 2026-12-18.
- 2026-11-28: #50 conflicted with develop after #49 merged (R50-1, PROJECT_STATE.md only); resolved in the #50 sync with develop's version. TV1 chose §7.5 option (a) for GOV-14.
- 2026-11-28 closeout: merged branches from #42–#48 no longer exist locally or on origin (checked after `git fetch --prune`). Open PRs #35/#36/#39 are CHANGES_REQUESTED, CI 4/4, 7–12 commits behind `develop`.

## Resume

- Completed: GOV-01, GOV-02 (verified); GOV-03, GOV-04 (merged in fcf7919); GOV-05 (applied outside repo); API-01 merged (5d03acb, unreviewed, GOV-13); local `develop` synced
- Pending: FE-AU commit/push/PR (TV1, D13) + TV1 UI checklist; GOV-14/15/16 §7.5 (Codex `git diff M S`); author fixes on #35/#36/#39; root-cause decision on the admin bypass; GOV-08 to GOV-13 Codex reviews + §7.5 records; A4/A5 reruns; token renewal before 2026-12-18
- REV-49-51 (approved by TV1 2026-11-28): #49 reviewed (BLOCKED on R49-1 decision, no code bug), #50 reviewed (READY code; LOW/NIT only); #51 reviewed (READY, no findings). Claude review part done. Details: docs/reviews/PR_REVIEW.md
- Codex recommendations (source: "khuyến nghị kỹ thuật của Codex tại SHA đã kiểm tra", forwarded by TV1) recorded in PR_REVIEW.md: #49 @ 85ecb02 REQUEST_CHANGES (history; recheck pending on the new head); #50 @ f222868 COMMENT; #51 @ 2887f76 no finding. Codex did not rerun Gradle/pgTest. This pass reviewed only the #49/#50/#51 diffs; GOV-08..13 unchanged. GitHub reviews on #49/#50/#51: 0; TV1 ACCEPT: none (checked 2026-11-28)
- Next action: TV1 runs the FE-AU UI checklist, commits and pushes `feature/fe-auth`, opens the PR (this file carries the GOV-15/16 records); Codex reviews `git diff 1eff662 4b44492` (GOV-15) and Codex reviews `git diff a3e531f 1a91375` (GOV-16, M per TV1 choice (i)); peer review or D12 waiver + ACCEPT for FE-AU; TV1 pings Khoi12vn. Governance items after.

Update after each completed task. Revalidate stale information when relevant code changes. Never mark unverified work as VERIFIED. Avoid duplicate analysis.
