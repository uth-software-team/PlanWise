# Audit State

Persistent audit state. Follow CHECKLIST.md. Update after each completed module. Keep entries compact and evidence-based.

## Context

- Stack: Spring Boot 4.1.2 / Java 25 / Spring Security OAuth2 resource server (HS256 JWT), JPA + Flyway, PostgreSQL 16 / H2; React + Vite frontend
- Architecture: modular monolith (`group16.webapp.modules.*`), context path `/api`, base `/api/v1`; stateless JWT, per-group roles enforced in services (D8)
- Scope: backend `Code/backend/src/main`, frontend `Code/frontend/src`, CI, compose, env examples. Implemented on this branch: auth/profile, group (GR-01..04), schedule. Stubs (NOT_IMPLEMENTED): task, availability/fixed events/calendar, assignment, AI, notification (NoOp)
- Branch: audit on `feature/common-contract-max-query-range` (8e7db4b); fixes on `fix/auth-jwt-hardening` from origin/develop 5d03acb (PR #48 merged; audited files identical)
- Baseline commit: 8e7db4b
- Last audited commit: 5d03acb + uncommitted SA-02/SA-04 fix
- Working tree: SA-02/SA-04 changes + this file, uncommitted (TV1 commits, D13)
- Phase: COMPLETE for SA-02/SA-04; other items pending
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
| 3 Route protection / state / async | PENDING | ProtectedRoute.tsx, scheduleApi.ts not reviewed in depth |
| 4 Business logic / transactions / concurrency (schedule, group) | REVIEWED | Proposal `@Version` + FOR UPDATE on confirm; partial unique index one PROPOSED per user (V10); D14 span caps; OptimisticLock → 409 STALE_VERSION |
| 4 Constraints / integrity | REVIEWED | V7, V10 FKs, CHECKs, composite FKs (proposal_id,user_id) |
| 4 DB privileges / backup | N/A | Local dev DB only; no deploy config |
| 5 Hardcoded credentials / secret history | REVIEWED | Only `.env.example` tracked; history `JWT_SECRET` = placeholder → SA-02 |
| 5 Known CVEs | NOT_RUN | Needs `npm audit` / dependency-check (network) — approval required |
| 6 CI/CD permissions | REVIEWED | gitflow-check.yml: `pull_request` only, `contents: read`, no secrets |
| 6 Docker / ports | REVIEWED | Dockerfile empty (0 bytes); compose binds 127.0.0.1 only |
| 6 Prod misconfig | PARTIAL | Swagger public, no prod profile → SA-08 |
| 7 Performance | PARTIAL | N+1 user lookups → SA-09 |
| 8 Architecture & quality | PENDING | Not reviewed |
| Stub modules (task, availability, assignment, AI) | BLOCKED | Implementations in open PRs #35/#36/#39; re-audit after merge |

Status: PENDING / REVIEWED / PARTIAL / BLOCKED / N/A.

## Baseline

| Check | Result | Evidence |
|---|---|---|
| `./gradlew --offline build` (H2 tests) | PASS | 2026-11-28 on 8e7db4b: BUILD SUCCESSFUL (tasks up-to-date from previous run on same inputs); 375 tests, 0 failures/errors/skipped |
| `./gradlew pgTest` | NOT_RUN | Not run this session |
| Frontend lint / build | NOT_RUN | Not run this session |
| Dependency CVE scan | NOT_RUN | Needs approval (network) |

## Findings

| ID | Severity | Status | File:Line | Root Cause | Evidence | Impact | Fix |
|---|---|---|---|---|---|---|---|
| SA-01 | MEDIUM | DEFERRED | AuthServiceImpl.java:69-80; SecurityConfig.java:50-53 | No throttling/lockout on public login/register (CWE-307) | grep: no limiter; login only checks BCrypt | Online password guessing, account enumeration via register 409, signup spam | Option A: in-memory per-email failure counter (e.g. 5 fails/15 min → 429) in AuthServiceImpl + new ErrorCode `TOO_MANY_REQUESTS` (contract change). Option B: DEFER (accept for MVP) |
| SA-02 | LOW | VERIFIED | SecurityConfig.java:126-137; Code/backend/.env.example:9 | Placeholder `change-me-...` (49 bytes) passes the ≥32-byte check (CWE-1188) | `.env.example` value; setup-dev.sh:128-135 only replaces it when openssl exists | Copied `.env` → publicly known HS256 key → forged tokens for any user id | Reject secrets starting with `change-me` in `validateJwtSecret` + unit test |
| SA-03 | LOW | CONFIRMED | GroupRequest.java (description); GenerateScheduleRequest.java (includeTaskIds) | No size bound on `description` (TEXT) or `includeTaskIds` list; no max body size (CWE-770) | DTOs have no `@Size`; V7:12 `TEXT` | Large payloads stored/processed | `@Size(max=N)` on both (N needs TV1: e.g. 1000 chars / 500 ids) + OpenAPI snapshot re-export |
| SA-04 | INFO | VERIFIED | SecurityConfig.java:88-91 | Decoder validates exp/nbf only, not `iss` | JwtService sets `iss=planwise`; decoder uses defaults | Minimal (single shared key) | `setJwtValidator(JwtValidators.createDefaultWithIssuer("planwise"))` |
| SA-05 | INFO | CONFIRMED | AuthController.java:47-52 | Stateless 24h JWT, no revocation (G7, D11, CG-04) | Accepted team decision | Stolen token valid until expiry; ScheduleService has no active-user check (no deactivation feature exists yet) | None (accepted) |
| SA-06 | INFO | CONFIRMED | Code/frontend/src/lib/auth-token.ts:3-50 | JWT in localStorage (CG-04) | Accepted decision; no XSS sinks found | XSS would expose token | None (accepted); keep no-raw-HTML rule |
| SA-07 | INFO | CONFIRMED | GroupServiceImpl.java:93-110; GroupAvailabilityServiceImpl.java:41-60 | Leader adds any registered email without consent (G8); members see all members' BUSY intervals (CG-11) | Accepted decisions | Any user can see another user's busy times for ≤366 days; email existence oracle (404 vs 200) | None (accepted); flag for post-MVP invite flow |
| SA-08 | INFO | CONFIRMED | SecurityConfig.java:52; application.properties:37 | Swagger/OpenAPI public, no prod profile | permitAll list | API surface disclosure if deployed | Disable springdoc in a future prod profile |
| SA-09 | LOW | CONFIRMED | GroupServiceImpl.java:236-238 | Per-member `userApi.findById` (N+1) | stream of findById | Slow for large groups only | `findAllById` batch lookup (optional) |

Severity: CRITICAL / HIGH / MEDIUM / LOW / INFO.

Status: POTENTIAL / CONFIRMED / FALSE_POSITIVE / FIXED / VERIFIED / UNVERIFIED / DEFERRED.

## Fix Plan

| ID | Changes | Risk | Test | Rollback | Approval |
|---|---|---|---|---|---|
| SA-02 | SecurityConfig.validateJwtSecret: reject `change-me` prefix (case-insensitive, trimmed) | Low; devs with placeholder must regenerate secret | SecurityConfigJwtSecretTests +2 | Revert commit | APPROVED (TV1 2026-11-28) |
| SA-04 | jwtDecoder: `JwtValidators.createDefaultWithIssuer(JwtService.ISSUER)`; test token helpers set `iss` | Low; JwtService always sets iss=planwise | SecurityConfigIntegrationTests +2 (other iss, no iss → 401) | Revert commit | APPROVED (TV1 2026-11-28) |
| SA-01 | Option B: accept for MVP, revisit post-MVP | Brute force remains possible | — | — | REJECTED fix / DEFERRED (TV1 2026-11-28) |
| SA-03 | `@Size` on 2 DTO fields + OpenAPI re-export | Contract change (limits) | Validation tests 400 INVALID_INPUT | Revert commit | PENDING (TV1: limits) |
| SA-09 | Batch user lookup | Low | Existing group tests | Revert commit | PENDING (optional) |

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

Result: PASS / FAIL / NOT_RUN / BLOCKED / N/A.

## Resume

- Completed: DISCOVER, BASELINE (H2 build), AUDIT of auth, group, schedule, secrets, CI, frontend storage; SA-02, SA-04 fixed + verified
- Pending: TV1 commit/PR of `fix/auth-jwt-hardening`; decisions on SA-03 (limits), SA-09 (optional), CVE scan (network approval); checklist 3 (route/state/async), 7, 8; CVE scan (approval); pgTest baseline; re-audit task/availability/assignment/AI after PRs #35/#36/#39 merge
- Approved IDs: SA-02, SA-04 (done); SA-01 → option B (deferred)
- Blockers: None
- Decisions: SA-05..SA-08 treated as accepted by G7/G8/D11/CG-04/CG-11
- Remaining risks: SA-01 brute force (accepted); unreviewed stub modules; dependency CVEs unknown
- Next: TV1 commits/PRs the fix; then SA-03 decision or checklist 3/7/8

Resume pending work. Revalidate stale evidence when relevant files, configuration, dependencies, or requirements change. Store large sanitized reports separately. Never mark untested checks as PASS.
