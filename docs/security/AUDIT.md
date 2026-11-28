# Audit State

Persistent audit state. Follow CHECKLIST.md. Update after each completed module. Keep entries compact and evidence-based.

## Context

- Stack: Spring Boot 4.1.2 / Java 25 / Spring Security OAuth2 resource server (HS256 JWT), JPA + Flyway, PostgreSQL 16 / H2; React + Vite frontend
- Architecture: modular monolith (`group16.webapp.modules.*`), context path `/api`, base `/api/v1`; stateless JWT, per-group roles enforced in services (D8)
- Scope: backend `Code/backend/src/main`, frontend `Code/frontend/src`, CI, compose, env examples. Implemented on this branch: auth/profile, group (GR-01..04), schedule. Stubs (NOT_IMPLEMENTED): task, availability/fixed events/calendar, assignment, AI, notification (NoOp)
- Branch: audit on `feature/common-contract-max-query-range` (8e7db4b); fixes on `fix/auth-jwt-hardening` from origin/develop 5d03acb (PR #48 merged; audited files identical)
- Baseline commit: 8e7db4b
- Last audited commit: 65b2d63 (checklist 7); 8f38236 (frontend checklist 3); SA-10/SA-11 fix 2887f76 on `fix/frontend-index-and-token-bar` (from origin/develop 5d03acb)
- Working tree: TV1 commits (D13). SA-15/SA-17/SA-20 fixes merged into `develop` with PR #49 as 0df1ec9 (2026-11-28 09:35Z; tree == PR head 43f8500); merge had no approving review (GOV-14)
- Phase: COMPLETE for SA-02/SA-04/SA-10/SA-11; SA-15/SA-17/SA-20 FIXED and merged (0df1ec9); VERIFIED after the GOV-14 §7.5 option (a) check (build/pgTest on 0df1ec9 + Codex review of `git diff M S`); other items pending
- Updated: 2026-11-28

## Coverage

| Checklist Item / Module | Status | Evidence |
|---|---|---|
| 1 Auth bypass / token lifecycle | REVIEWED | SecurityConfig.java:50-90 (public list, anyRequest authenticated, HS256 decoder); JwtService.java:35-48 |
| 1 Password hashing | REVIEWED | BCrypt (SecurityConfig.java:99), 72-byte cap, timing equalizer AuthServiceImpl.java:41,71-75 |
| 1 Password recovery | N/A | `/v1/auth/password/**` permitted but no controller yet (BR-14 not implemented) |
| 1 RBAC / IDOR / privilege escalation | REVIEWED | Group: membership/leader checks + group row lock GroupServiceImpl.java:83-149,186-230. Schedule: every query scoped by `userId` (ScheduleService.java:183,207,214,263,289,313) |
| 1 Rate limiting | REVIEWED | None found (grep rate/limit/bucket/lockout: 0 hits) → SA-01 |
| 2 Injection | REVIEWED | Only derived queries + 2 parameterized JPQL (TeamGroupRepository.java:19, ScheduleProposalRepository.java:25); no native SQL |
| 2 CORS / headers / CSRF | REVIEWED | Explicit origins + credentials (SecurityConfig.java:104-113); CSRF off acceptable (Bearer header, no cookies); Spring default headers |
| 2 Input validation / limits | PARTIAL | DTOs reviewed for group/schedule/auth → SA-03 |
| 2 Error leakage | REVIEWED | GlobalExceptionHandler: generic 500, SQL cause class only logged |
| 2 File upload / path traversal | N/A | No upload endpoint; FileUtils unused |
| 3 Frontend DOM / token storage | REVIEWED | No innerHTML/dangerouslySetInnerHTML/eval; JWT in localStorage (auth-token.ts:3, CG-04) → SA-06 |
| 3 Env exposure | REVIEWED | Only `VITE_API_BASE_URL` used (apiClient.ts:72); built bundle has no other `VITE_*`; no sourcemaps in dist; dist untracked |
| 3 Route protection | REVIEWED | ProtectedRoute/PublicOnlyRoute react to token presence; 401 clears token (apiClient.ts:93-97); real authz is server-side → SA-13 |
| 3 State consistency / async races | REVIEWED | scheduleApi.ts:153-404: tickets drop stale responses, actions bound to account, token re-checked before send; buttons disabled while busy |
| 3 Error handling / edge cases | REVIEWED | Network errors → fallback messages; non-JSON bodies handled; no timeout → SA-12; index.html → SA-10; dev token bar → SA-11 |
| 3 CSP | N/A | No hosting config yet; revisit with deployment |
| 4 Business logic / transactions / concurrency (schedule, group) | REVIEWED | Proposal `@Version` + FOR UPDATE on confirm; partial unique index one PROPOSED per user (V10); D14 span caps; OptimisticLock → 409 STALE_VERSION |
| 4 Constraints / integrity | REVIEWED | V7, V10 FKs, CHECKs, composite FKs (proposal_id,user_id) |
| 4 DB privileges / backup | N/A | Local dev DB only; no deploy config |
| 5 Hardcoded credentials / secret history | REVIEWED | Only `.env.example` tracked; history `JWT_SECRET` = placeholder → SA-02 |
| 5 Known CVEs | NOT_RUN | Needs `npm audit` / dependency-check (network) — approval required |
| 6 CI/CD permissions | REVIEWED | gitflow-check.yml: `pull_request` only, `contents: read`, no secrets |
| 6 Docker / ports | REVIEWED | Dockerfile empty (0 bytes); compose binds 127.0.0.1 only |
| 6 Prod misconfig | PARTIAL | Swagger public, no prod profile → SA-08 |
| 7 Memory/resource leaks | REVIEWED | FE: both `storage` listeners removed on cleanup (SchedulePage.tsx:62-69, auth-token.ts:65-79); no timers. BE: no caches/static collections growing per request |
| 7 N+1 queries | REVIEWED | SA-09 (deferred); task titles batched (ScheduleService.java:444-449); per-member calendar fan-out → SA-16 |
| 7 Inefficient operations | REVIEWED | Unbounded history load → SA-15. Acceptable at MVP scale: SlotCalculator.subtractBusy O(A×B) (SlotCalculator.java:23-38), isContainedWithin re-merges per session (ConstraintValidator.java:61 → SlotCalculator.java:65-72), engine O(T×P); inputs capped by D14 (366 days) |
| 7 Pagination / cache invalidation | REVIEWED | `/tasks` page size ≤100 (stub); group lists/members unpaginated but bounded by membership; no caching → N/A |
| 7 Timeout / retry / failure | REVIEWED | No DB/transaction timeouts → SA-18; FE fetch timeout SA-12 (deferred) |
| 7 Concurrent requests | PARTIAL | Tested: concurrent confirm (ScheduleServiceIntegrationTests.java:507), leader leave (GroupMembershipConcurrencyPgTests.java:81). Untested → SA-17 |
| 7 Resource exhaustion | REVIEWED | Query windows capped (D14); SA-03 (deferred); group fan-out → SA-16; history growth → SA-19 |
| 8 Architecture & quality | PENDING | Not reviewed |
| Stub modules (task, availability, assignment, AI) | BLOCKED | Implementations in open PRs #35/#36/#39; re-audit after merge |

Status: PENDING / REVIEWED / PARTIAL / BLOCKED / N/A.

## Baseline

| Check | Result | Evidence |
|---|---|---|
| `./gradlew --offline build` (H2 tests) | PASS | 2026-11-28 on 8e7db4b: BUILD SUCCESSFUL (tasks up-to-date from previous run on same inputs); 375 tests, 0 failures/errors/skipped |
| `./gradlew pgTest` | NOT_RUN | Not run this session |
| Frontend lint / tsc / build | PASS | 2026-11-28 on 8f38236: `eslint .` rc 0, `tsc -b` rc 0, `npm run build` OK (484 kB JS) |
| `npm run check:api-client` | PASS | 10/10 (T1–T9b, fake fetch) |
| Dependency CVE scan | NOT_RUN | Needs approval (network) |

## Findings

| ID | Severity | Status | File:Line | Root Cause | Evidence | Impact | Fix |
|---|---|---|---|---|---|---|---|
| SA-01 | MEDIUM | DEFERRED | AuthServiceImpl.java:69-80; SecurityConfig.java:50-53 | No throttling/lockout on public login/register (CWE-307) | grep: no limiter; login only checks BCrypt | Online password guessing, account enumeration via register 409, signup spam | Option A: in-memory per-email failure counter (e.g. 5 fails/15 min → 429) in AuthServiceImpl + new ErrorCode `TOO_MANY_REQUESTS` (contract change). Option B: DEFER (accept for MVP) |
| SA-02 | LOW | VERIFIED | SecurityConfig.java:126-137; Code/backend/.env.example:9 | Placeholder `change-me-...` (49 bytes) passes the ≥32-byte check (CWE-1188) | `.env.example` value; setup-dev.sh:128-135 only replaces it when openssl exists | Copied `.env` → publicly known HS256 key → forged tokens for any user id | Reject secrets starting with `change-me` in `validateJwtSecret` + unit test |
| SA-03 | LOW | DEFERRED | GroupRequest.java (description); GenerateScheduleRequest.java (includeTaskIds) | No size bound on `description` (TEXT) or `includeTaskIds` list; no max body size (CWE-770) | DTOs have no `@Size`; V7:12 `TEXT` | Large payloads stored/processed | `@Size(max=N)` on both (N needs TV1: e.g. 1000 chars / 500 ids) + OpenAPI snapshot re-export |
| SA-04 | INFO | VERIFIED | SecurityConfig.java:88-91 | Decoder validates exp/nbf only, not `iss` | JwtService sets `iss=planwise`; decoder uses defaults | Minimal (single shared key) | `setJwtValidator(JwtValidators.createDefaultWithIssuer("planwise"))` |
| SA-05 | INFO | CONFIRMED | AuthController.java:47-52 | Stateless 24h JWT, no revocation (G7, D11, CG-04) | Accepted team decision | Stolen token valid until expiry; ScheduleService has no active-user check (no deactivation feature exists yet) | None (accepted) |
| SA-06 | INFO | CONFIRMED | Code/frontend/src/lib/auth-token.ts:3-50 | JWT in localStorage (CG-04) | Accepted decision; no XSS sinks found | XSS would expose token | None (accepted); keep no-raw-HTML rule |
| SA-07 | INFO | CONFIRMED | GroupServiceImpl.java:93-110; GroupAvailabilityServiceImpl.java:41-60 | Leader adds any registered email without consent (G8); members see all members' BUSY intervals (CG-11) | Accepted decisions | Any user can see another user's busy times for ≤366 days; email existence oracle (404 vs 200) | None (accepted); flag for post-MVP invite flow |
| SA-08 | INFO | CONFIRMED | SecurityConfig.java:52; application.properties:37 | Swagger/OpenAPI public, no prod profile | permitAll list | API surface disclosure if deployed | Disable springdoc in a future prod profile |
| SA-09 | LOW | DEFERRED | GroupServiceImpl.java:236-238 | Per-member `userApi.findById` (N+1) | stream of findById | Slow for large groups only | `findAllById` batch lookup (optional) |
| SA-10 | LOW | VERIFIED | Code/frontend/index.html:1-25 | PR #31 (6102549) prepended a second HTML document to the FE-00 one | File has 2 `<html>` docs; built dist/index.html keeps 2 `<div id="root">`, first `lang="en"` | Invalid HTML, duplicate id, wrong document language for a Vietnamese UI (a11y/screen readers), inconsistent title | Restore the single FE-00 document (95236dc: `lang="vi"`, title "PlanWise AI") |
| SA-11 | LOW | VERIFIED | Code/frontend/src/features/schedule/components/SchedulePage.tsx:43-45,146-151,177-193 | "Standalone testing" JWT paste bar ships in production builds (not gated by `import.meta.env.DEV`) | Button always rendered; Apply calls `setAuthToken` | Users taught to paste tokens; a pasted attacker token silently switches the account (CWE-451/login-CSRF style) | Render the button/bar only when `import.meta.env.DEV`, or remove once FE-AU login exists |
| SA-12 | LOW | DEFERRED | Code/frontend/src/lib/apiClient.ts:109-112 | `fetch` without timeout/abort signal | No `AbortSignal` anywhere in src | Hung backend keeps `isBusy` true → all schedule actions disabled until reload | Default `AbortSignal.timeout(15000)` when caller passes no `signal`; map abort to `ApiError` (needs check:api-client test) |
| SA-13 | INFO | POTENTIAL | Code/frontend/src/routes/ProtectedRoute.tsx:11-12 | `?redirect=` built now; consumer (FE-AU login) not written yet | Login page is a placeholder | Open redirect (CWE-601) if FE-AU navigates to it unchecked | FE-AU: accept only paths starting with `/` and not `//` or `/\`; else go to `/tasks` |
| SA-14 | INFO | CONFIRMED | Code/frontend/src/app/layouts/UserMenu.tsx:19-21 | Logout clears the token but not `planwise.schedule.activeProposalId` (sessionStorage) | Next account in the same tab re-reads it → 404 → cleared (scheduleApi.ts:251-254) | None (server ownership check); cosmetic | None |
| SA-15 | LOW | FIXED (#49, 827b9f0) | Code/backend/src/main/java/group16/webapp/modules/schedule/service/ScheduleService.java:429-442 | `computeRemainingMinutes` loads every CONFIRMED/COMPLETED session the user ever had (`findByUserIdAndStatusIn`), not only those of the tasks being planned | Called by generate (:118) and checkHardConstraints (:408, edit + confirm) | Work and memory per request grow with account history; confirm does it while holding the proposal row lock | Query only the target task ids (`findByUserIdAndTaskIdInAndStatusIn`, index `idx_schedule_sessions_task` exists) or an aggregate SUM per task; no schema change |
| SA-16 | LOW | DEFERRED | Code/backend/src/main/java/group16/webapp/modules/group/service/impl/GroupAvailabilityServiceImpl.java:55-58 | One `busyIntervals` expansion per ACTIVE member over up to 366 days; no member cap on a group (GroupServiceImpl.java:93-110) | CalendarApi has no implementation on this branch (501 NOT_IMPLEMENTED) | Any member of a large group can trigger members × 366-day expansions per request | Re-check after #39 lands CalendarApi; options: member cap, or a shorter max window for group availability (contract change) |
| SA-17 | INFO | FIXED (#49, 398aa49) | ScheduleService.java:79-102,200-255,310-347 | Concurrent generate/generate and edit/confirm on one batch have no tests | Expected safe: `@Version` on ScheduleProposal (ScheduleProposal.java:38) → 409 STALE_VERSION; partial unique index `uk_schedule_proposals_active_user` (V10) → 409 DATA_CONFLICT. H2 tests cannot exercise the partial index | A regression could leave 2 PROPOSED batches or move a confirmed session unnoticed | Add pgTest cases (two threads, CountDownLatch) for both races |
| SA-18 | INFO | CONFIRMED | Code/backend/src/main/resources/application.properties | No transaction/statement timeout; Hikari defaults (pool 10, connectionTimeout 30 s) | No `spring.transaction.default-timeout`, no `statement_timeout` | A slow query or lock wait holds a pooled connection indefinitely | Optional: `spring.transaction.default-timeout` (config change); not needed for MVP |
| SA-19 | INFO | CONFIRMED | ScheduleService.java:92-102,139-146 | Each generate keeps the old batch and its sessions as CANCELLED rows; no retention | No delete/cleanup path | Table growth only; queries filter by status/user and are indexed | None for MVP |
| SA-20 | LOW | FIXED (#49, 827b9f0) | Code/backend/src/main/java/group16/webapp/modules/schedule/service/ScheduleService.java:214,250,270,279,313 (at 5d03acb) | Opposite lock order: `editSession`/`deleteSession` read the proposal without a lock (`findByIdAndUserId`, :214/:270) and at flush write/delete the session row (:250/:279) before the proposal row (version bump); `confirmProposal` locks the proposal first (`findByIdAndUserIdForUpdate`, :313), then writes the sessions | Code read at 5d03acb; deadlock not reproduced in a test | Concurrent edit/delete and confirm of one batch can deadlock; PostgreSQL aborts one transaction (40P01), so the user gets an error instead of 409. Data stays consistent (rollback) | Edit and delete lock the proposal row first (`findByIdAndUserIdForUpdate`), same order as confirm |

Severity: CRITICAL / HIGH / MEDIUM / LOW / INFO.

Status: POTENTIAL / CONFIRMED / FALSE_POSITIVE / FIXED / VERIFIED / UNVERIFIED / DEFERRED.

## Fix Plan

| ID | Changes | Risk | Test | Rollback | Approval |
|---|---|---|---|---|---|
| SA-02 | SecurityConfig.validateJwtSecret: reject `change-me` prefix (case-insensitive, trimmed) | Low; devs with placeholder must regenerate secret | SecurityConfigJwtSecretTests +2 | Revert commit | APPROVED (TV1 2026-11-28) |
| SA-04 | jwtDecoder: `JwtValidators.createDefaultWithIssuer(JwtService.ISSUER)`; test token helpers set `iss` | Low; JwtService always sets iss=planwise | SecurityConfigIntegrationTests +2 (other iss, no iss → 401) | Revert commit | APPROVED (TV1 2026-11-28) |
| SA-01 | Option B: accept for MVP, revisit post-MVP | Brute force remains possible | — | — | REJECTED fix / DEFERRED (TV1 2026-11-28) |
| SA-03 | `@Size` on 2 DTO fields + OpenAPI re-export | Contract change (limits) | Validation tests 400 INVALID_INPUT | Revert commit | REJECTED / DEFERRED (TV1 2026-11-28) |
| SA-10 | Replace index.html with the FE-00 single document | Low; title/lang change visible | `npm run build`, dist/index.html has 1 root | Revert commit | APPROVED (TV1 2026-11-28) |
| SA-11 | Gate token bar with `import.meta.env.DEV` | Low; testers on prod build lose the bar (no login UI until FE-AU) | lint/tsc/build; check bundle has no "Bearer Token:" | Revert commit | APPROVED (TV1 2026-11-28) |
| SA-12 | Default fetch timeout in apiRequest | Medium; slow generate calls may abort (pick timeout) | New check:api-client case | Revert commit | REJECTED / DEFERRED (TV1 2026-11-28) |
| SA-15 | Repository method `findByUserIdAndTaskIdInAndStatusIn`; `computeRemainingMinutes` passes the task ids it computes for | Low; same result, fewer rows | Existing schedule tests (remaining minutes, HC-07) + build/pgTest | Revert commit | APPROVED (TV1 2026-11-28) |
| SA-17 | 2 pgTest cases: concurrent generate (one PROPOSED remains, other 409), edit vs confirm (one wins, other 409, confirmed session unchanged) | Low (tests only); flakiness risk with threads | `./gradlew pgTest` | Revert commit | APPROVED (TV1 2026-11-28) |
| SA-20 | `editSession`/`deleteSession`: `findByIdAndUserIdForUpdate` before any session write (same order as confirm); ScheduleServiceTest edit and confirm stubs moved to the locking method | Low; edit/delete now wait on a confirm of the same batch, then get 409 | ScheduleServiceTest edit cases (:126, `stubEditableBatch` :190) and confirm case (:214) stub the locking method; contract test `editDuringConfirmOfSameBatchIsRejectedAndConfirmedSessionStaysPut` (pg). No test covers the lock in `deleteSession` | Revert commit | APPROVED (TV1 2026-11-28) |
| SA-09 | Batch user lookup | Low | Existing group tests | Revert commit | REJECTED (TV1 2026-11-28: skip) |

Approval: PENDING / APPROVED / REJECTED.

## Verification

| ID / Check | Before | After | Evidence |
|---|---|---|---|
| SA-02 | Placeholder accepted (49 bytes ≥ 32) | PASS | `rejectsEnvExamplePlaceholderSecret`, `rejectsPlaceholderPrefixIgnoringCaseAndSurroundingSpace` pass |
| SA-04 | Any iss accepted | PASS | `tokenFromAnotherIssuerReturns401`, `tokenWithoutIssuerReturns401` pass; `validTokenPassesSecurityFilter` still 404 with iss=planwise |
| Regression H2 | 375 pass (8e7db4b) | PASS | 2026-11-28 `./gradlew --offline build --rerun-tasks`: 379 tests, 0 fail/error/skip |
| Regression pgTest | NOT_RUN | PASS | 2026-11-28 `./gradlew --offline pgTest --rerun-tasks`: 167 tests, 0 fail/error/skip |
| IDE diagnostics (4 changed files) | — | PASS | VS Code Problems: 0 warnings/errors (may be unanalyzed if not open) |
| New tests fail without fix | — | NOT_RUN | Inferred only (other-iss token would reach MVC → 404; placeholder would not throw) |
| SA-10 (2887f76) | dist/index.html: 2 `<html>`, 2 `#root`, `lang="en"` | PASS | 2026-11-28 `npm run build`: 1 `<html>`, 1 `#root`, `lang="vi"`; body styled by index.css base layer |
| SA-11 (2887f76) | Bundle contains token bar | PASS | Prod bundle: 0 hits for "Bearer Token:", "Paste JWT token", "Apply & Reload", "JWT Token"; `NODE_ENV=development` build (scratchpad): bar present |
| Frontend regression (2887f76) | PASS (8f38236) | PASS | prettier --check (2 files), `eslint .` rc 0, `tsc -b` rc 0, build OK, check:api-client 10/10 |
| IDE diagnostics (2 files) | — | PASS | index.html clean; SchedulePage.tsx 1 pre-existing Tailwind hint (`min-w-[260px]` → `min-w-65`), unchanged line, out of scope |
| SA-15 (827b9f0, #49) | Whole session history loaded | PASS | `computeRemainingMinutes` queries only the planned task ids; ScheduleServiceTest verifies `findByUserIdAndTaskIdInAndStatusIn(1, {10}, …)`; same results in existing schedule tests |
| SA-17 (398aa49 + 24ff666, #49) | No concurrency tests | PASS | `ScheduleServiceConcurrencyPgTests` (PostgreSQL 16). Edit vs confirm: confirm holds the proposal row lock in an open transaction; the edit must not finish within 500 ms, then exactly one wins and the other gets 409; a CONFIRMED session is never moved. Regenerate vs regenerate (24ff666; the 398aa49 barrier version was still timing-dependent, because generate re-queries the PROPOSED batch): generate 1 runs in an open transaction (old batch cancelled, row lock held); generate 2 still reads the old batch and blocks on that lock; the test waits for the lock wait in `pg_stat_activity`, then lets generate 1 commit → generate 2 gets 409 STALE_VERSION, and generate 1's batch is the only PROPOSED one. Local XML (code == 24ff666): both case names in `build/test-results/pgTest`, 0 failures/errors; class-only reruns 5× green (09:17:41–09:18:13Z) before the full run |
| SA-20 (827b9f0, #49) | Opposite lock order | PASS (partial) | ScheduleServiceTest stubs `findByIdAndUserIdForUpdate` in the edit cases (:126, `stubEditableBatch` :190) and the confirm case (:214); for edit, an unstubbed `findByIdAndUserId` returns empty → 404, so a revert fails. The pg contract test covers edit vs confirm only. No unit or integration test reaches the lock in `deleteSession` (`confirmedSessionCannotBeEditedOrDeleted` stops at the status check); TV1 chose not to add one (2026-11-28). The deadlock interleaving itself is not reproduced: the contract test checks the outcome, not the lock order |
| Regression #49 | H2 377, pg 167 (827b9f0) | PASS | Local (code == 24ff666), counts read from the XML on disk: full `./gradlew --offline --rerun-tasks build pgTest` run last (2026-11-28, 9/9 tasks) → `test` 26 files, 375 tests (09:18:29–09:18:40Z); `pgTest` 7 files, 169 tests (09:18:40–09:18:47Z); 0 failures/errors/skipped (2 cases moved H2 → pg). CI: 4/4 SUCCESS on #49 f161d6c (run 46129174055, code == 398aa49); CI logs have no per-test names and no report artifact, so case names come from the local XML only. The TV1-forwarded recheck used the backend XML already on the machine; it did not rerun Gradle, Docker or a production build |

Result: PASS / FAIL / NOT_RUN / BLOCKED / N/A.

## Resume

- Completed (fixes on #49): SA-15 (827b9f0), SA-20 (827b9f0), SA-17 (398aa49)
- Completed: DISCOVER, BASELINE (H2 build), AUDIT of auth, group, schedule, secrets, CI, frontend storage; SA-02, SA-04 fixed + verified (committed 4faab9f, 8f38236); checklist 3 frontend reviewed; checklist 7 reviewed; SA-10, SA-11 fixed + verified (2887f76)
- Pending: GOV-14 option (a) for #49 (merged 0df1ec9); review/ACCEPT/merge of PRs #50 (SA-02/SA-04) and #51 (SA-10/SA-11); post-merge build/pgTest for VERIFIED; CVE scan (network approval); SA-16 re-check after #39; checklist 8; pgTest baseline; re-audit task/availability/assignment/AI after PRs #35/#36/#39 merge
- Approved IDs: SA-02, SA-04 (done); SA-01 → option B (deferred); SA-03 deferred; SA-09 skipped; SA-10, SA-11 (done); SA-12 deferred; SA-15, SA-17, SA-20 approved and fixed on #49; SA-16 deferred until #39 (TV1 2026-11-28)
- Blockers: None
- Decisions: SA-05..SA-08 treated as accepted by G7/G8/D11/CG-04/CG-11
- Remaining risks: SA-01 brute force (accepted); SA-03 unbounded description/includeTaskIds (accepted); SA-09 N+1 (accepted); SA-12 no fetch timeout (accepted); unreviewed stub modules; dependency CVEs unknown
- Next: TV1 commits the #50 sync merge (origin/develop 0df1ec9 into this branch; PROJECT_STATE.md conflict resolved with develop's version, R50-1); CI + Codex recheck on the new #50 head; then checklist 8 or CVE scan (needs approval)

Resume pending work. Revalidate stale evidence when relevant files, configuration, dependencies, or requirements change. Store large sanitized reports separately. Never mark untested checks as PASS.
