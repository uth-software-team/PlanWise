# Pull Request Review State

Track active PR reviews. Keep concise and evidence-based.

## Reviews

| PR  | Base | Head SHA | CI  | Claude verdict | Claude findings | Codex (source + SHA) | GitHub review | TV1 ACCEPT |
| --- | ---- | -------- | --- | -------------- | --------------- | -------------------- | ------------- | ---------- |
| #49 | develop @ 5d03acb | f161d6c pushed; local 24ff666 (R49-2 re-fix) + this docs change not pushed yet (fix 827b9f0, tests 398aa49 + 24ff666) | 4/4 SUCCESS on f161d6c (run 46129174055); new head not run yet | BLOCKED (process: push, CI and Codex recheck on the new head; peer review or D12 waiver; TV1 ACCEPT). Code: no blocking bug. R49-1 fixed in 398aa49, R49-2 re-fixed in 24ff666, both by Claude itself, so not an independent review | R49-1 MEDIUM fixed; R49-2 MEDIUM re-fixed (24ff666); R49-3 NIT addressed (Javadoc); Q49-1 resolved | REQUEST_CHANGES @ 85ecb02 (history); recheck on the final head pending | REVIEW_REQUIRED, 0 reviews (checked 2026-11-28) | None (0 PR comments, checked 2026-11-28) |
| #50 | develop @ 5d03acb | 9ae6441 (f222868 + SA-20 AUDIT record) | 4/4 SUCCESS on f222868; CI on 9ae6441 in progress (2026-11-28) | READY (code: matches TV1-approved SA-02/SA-04 fixes; no secret exposure). Process: peer review or D12 waiver, Codex, TV1 ACCEPT; sync after #49 (R50-1). Tests not rerun in review | R50-1 LOW, R50-2 NIT, R50-3 SUGGESTION | COMMENT @ f222868 (history; head now 9ae6441, docs only; recheck pending) | REVIEW_REQUIRED, 0 reviews (checked 2026-11-28) | None (0 PR comments, checked 2026-11-28) |
| #51 | develop @ 5d03acb | 2887f76 | 4/4 SUCCESS | READY (code: index.html identical to FE-01 5126cee, lang=vi; token bar button + bar gated by `import.meta.env.DEV`; prod-bundle check recorded in AUDIT on #50). Process: peer review or D12 waiver, Codex, TV1 ACCEPT. Tests not rerun in review | None | No finding @ 2887f76 (tsc/ESLint only, no prod build) | REVIEW_REQUIRED, 0 reviews (checked 2026-11-28) | None (0 PR comments, checked 2026-11-28) |

Verdict: READY / CHANGES_REQUIRED / BLOCKED / UNVERIFIED.

The columns are separate sources: Claude review (this file), Codex recommendations, the official GitHub review, and the TV1 ACCEPT decision. None of them stands in for another.

## Codex Recommendations

Source of every row: "khuyến nghị kỹ thuật của Codex tại SHA đã kiểm tra" (forwarded by TV1). Not a Claude finding, GitHub review or TV1 decision.

| PR  | SHA checked | ID  | Severity | Recommendation | Conflicts with Claude finding | Status |
| --- | ----------- | --- | -------- | -------------- | ----------------------------- | ------ |
| #49 | 85ecb02119cb0e2c9b32d8d9850c2f40c5113508 | C49 | REQUEST_CHANGES | Overall: two MEDIUM findings open; fix R49-1 and R49-2 before merge | Claude verdict COMMENT/BLOCKED | OPEN |
| #49 | 85ecb02119cb0e2c9b32d8d9850c2f40c5113508 | R49-1 | MEDIUM | The edit/confirm and generate/generate tests run on H2; pgTest only selects `@Tag("pg")`, so 167/167 pg does not prove these cases ran through the service on PostgreSQL. Add both cases on PostgreSQL 16 and confirm each case name appears in the pgTest report | Agrees (Claude MEDIUM) | OPEN |
| #49 | 85ecb02119cb0e2c9b32d8d9850c2f40c5113508 | R49-2 | MEDIUM | The CyclicBarrier sits before `generate()`. If A commits before B reads the batch, B replaces A's batch and both succeed (valid, one PROPOSED remains), but line 639 demands a 409. If SA-17 targets the version conflict, sync both transactions after they have read the same old batch, then assert the 409 and the final state | Same issue; Claude LOW vs Codex MEDIUM; Codex wants it fixed before merge. **TV1 decides** | OPEN |
| #50 | f2228685838b12cce54825cf98e86dc697bb2df1 | C50 | COMMENT | No blocking issue in the JWT change; the decoder checks the issuer JwtService writes; tests cover valid token, wrong issuer, missing issuer and placeholder key | Agrees | — |
| #50 | f2228685838b12cce54825cf98e86dc697bb2df1 | R50-3 | LOW | Even with openssl, the Base64 secret can contain `/`; `scripts/setup-dev.sh` puts it into a `sed` command that also uses `/` as delimiter. Codex reproduced the sed error with dummy data. `.env.example` is copied first, so a rerun can keep the placeholder key. Separate fix: generate a hex key | Adds a case to Claude R50-3 (Claude: missing openssl, SUGGESTION) | OPEN |
| #51 | 2887f7697b5291595d71771592033fea22deb82f | C51 | — | No blocking finding: one `lang="vi"` document, one `#root`, one boot script; toggle button and JWT input both gated by `import.meta.env.DEV`; tsc 0 errors; ESLint on the changed TSX 0 errors, 0 warnings. Production build not run in this pass | Agrees | — |

The #49 rows are history for 85ecb02; #49 pushed head is f161d6cbcc5a3f38ed3b13bb048f781b40f22c8b; local commit 24ff666d1358bf742bd5c73245c5a97258df8b85 (R49-2 re-fix) is not pushed yet. The #50 rows are history for f222868; #50 head is now 9ae6441652963ffe965e0a91f1032bfc086445ac (AUDIT.md only; CI 4/4). Codex has not rechecked either final head.

Base for all rows: develop @ 5d03acb7877b7c11729ce480a25d0fdce6b476f3. Codex did not rerun Gradle/pgTest in this pass. Q49-1 resolved by TV1 (SA-20 approved, recorded on #50). If a head or base changes, these rows are history for that exact SHA; recheck the changed part.

## Findings

| PR  | ID  | Severity | File:Line | Issue | Suggested Fix | Status |
| --- | --- | -------- | --------- | ----- | ------------- | ------ |
| #49 | R49-1 | MEDIUM | ScheduleServiceIntegrationTests.java:535 | Approved SA-17 fix (docs/security/AUDIT.md on #50) is "2 pgTest cases"; both new tests are in the H2 `@SpringBootTest` class (no `@Tag("pg")`), pgTest count still 167. PostgreSQL lock behavior unverified | Move both cases to a `PostgresContainerTest` subclass, or TV1 accepts H2 coverage and updates the AUDIT SA-17 row | FIXED in 398aa49 (pushed; head 21fb22c): both cases moved to `ScheduleServiceConcurrencyPgTests` (PostgresContainerTest); local `./gradlew --offline --rerun-tasks build pgTest` H2 375/375, pg 169/169, both case names in the local pgTest report, class rerun 3× green (working tree == 398aa49 code). The pgTest XML of that run was later overwritten by class-only reruns; current evidence: local XML after the last full run `./gradlew --offline --rerun-tasks build pgTest` (2026-11-28, 9/9 tasks, run last so it is not overwritten): `build/test-results/test` 26 files, 375 tests, 09:18:29–09:18:40Z; `build/test-results/pgTest` 7 files, 169 tests, 09:18:40–09:18:47Z, both case names present; 0 failures/errors/skipped. Class-only reruns (5×, 09:17:41–09:18:13Z) green before that (code == 24ff666). CI on f161d6c (run 46129174055): 4/4 SUCCESS; CI log has no per-test names and no report artifact, so case names are verified locally only. Codex recheck pending |
| #49 | R49-2 | MEDIUM (TV1 2026-11-28; Claude first rated LOW) | ScheduleServiceIntegrationTests.java:638-639 | Regenerate test is flaky if B reads the PROPOSED batch after A commits: both succeed (correct), test demands one 409 | Hold A's transaction open with a latch, or assert invariants only (≥1 OK, any failure is 409, exactly 1 PROPOSED, original CANCELLED). Codex (source: khuyến nghị kỹ thuật của Codex tại SHA đã kiểm tra, 85ecb02): MEDIUM, fix before merge; TV1 kept MEDIUM | RE-FIXED in 24ff666 (local, not pushed). Recheck forwarded by TV1 2026-11-28 (used the backend XML already on the machine; no Gradle, Docker or production build rerun) reopened it; the 398aa49 fix was still timing-dependent: generate re-queries the PROPOSED batch, so if A commits before B's query, B cancels A's new batch and both succeed. Now generate 1 runs in an open transaction (old batch cancelled, row lock held, not committed); generate 2 starts, still reads the old batch and blocks on that lock; the test polls `pg_stat_activity` until a backend waits on `Lock`, asserts generate 2 is not done, then lets generate 1 commit → generate 2 STALE_VERSION; the remaining PROPOSED batch is generate 1's. Order fixed by the lock wait, not by timing. Evidence: local XML after the last full run `./gradlew --offline --rerun-tasks build pgTest` (2026-11-28, 9/9 tasks, run last so it is not overwritten): `build/test-results/test` 26 files, 375 tests, 09:18:29–09:18:40Z; `build/test-results/pgTest` 7 files, 169 tests, 09:18:40–09:18:47Z, both case names present; 0 failures/errors/skipped. Class-only reruns (5×, 09:17:41–09:18:13Z) green before that. Codex recheck pending |
| #49 | R49-3 | NIT | ScheduleServiceIntegrationTests.java:560 | Edit-vs-confirm test does not reproduce the deadlock interleaving; likely passes without the SA-20 fix (proposal `@Version` → 409). Inference, not executed. Lock-method use is covered by ScheduleServiceTest | Optional: say "contract test" in the Javadoc | ADDRESSED in 398aa49 (Javadoc: "Contract test") |
| #49 | Q49-1 | QUESTION | ScheduleService.java:214,273 | SA-20 (lock order) is not recorded or approved in AUDIT.md on any branch (PR body says so) | TV1 confirms approval; record SA-20 in AUDIT.md | RESOLVED: TV1 approved SA-20 (2026-11-28); recorded in AUDIT.md on #50 (9ae6441) |
| #50 | R50-1 | LOW | docs/PROJECT_STATE.md:36 (on #50) | #49 adds an SEC-01 row too; merging #50 after #49 conflicts on the same table | When syncing #50 with `develop`, drop #50's SEC-01 row and keep the #49 version | OPEN (at sync) |
| #50 | R50-2 | NIT | docs/security/AUDIT.md:132,137 (on #50) | Resume says SA-15/SA-17 still to be implemented by Claude; done on #49. SA-20 (lock order, #49) not recorded | Update Resume and add SA-20 when syncing after #49 (see Q49-1) | FIXED in 9ae6441: SA-20 added; SA-15/SA-17 marked FIXED (#49); Resume/Next/Approved IDs updated |
| #50 | R50-3 | SUGGESTION | scripts/setup-dev.sh:128-139 | Without openssl the script copies the placeholder `JWT_SECRET` and reports success; after SA-02 the app then fails at startup (clear message). Codex (source: khuyến nghị kỹ thuật của Codex tại SHA đã kiểm tra, f222868), LOW: with openssl, a Base64 secret containing `/` breaks the `sed` (same delimiter); `.env.example` is already copied, so a rerun can keep the placeholder | Optional: print a warning when openssl is missing; Codex: generate a hex key (separate fix) | OPEN (non-blocking) |
| #48 | R48-1 | MEDIUM | ScheduleService.java:381-387 | Edit/confirm expand availability over [min session start, max session end] with no cap; an edited session moved years ahead bypasses the 366-day rule once #36 adds `AvailabilityApi` (day loop AvailabilityService.java:111 on #36) | TV1 chose option A: `editSession` rejects 400 `INVALID_TIME_RANGE` when the post-edit batch span > 366 days (D14) | FIXED, merged in 5d03acb (D14) |
| #48 | R48-2 | LOW | Code/backend/CLAUDE.md:40-41 | "D12/D13 (backend)" reuse governance IDs D12/D13; D12 lists `/v1/calendar` and D13 describes #35, neither on develop yet | Renamed to D14/D15, D15 wording fixed, "effective with #39/#35" added; PR body line 26 still says D12/D13 (TV1 edits) | FIXED, merged in 5d03acb |

## Merge Order

- Pending (TV1): #49 → #50 → #51 (#51 touches no state file, can merge any time). #49 and #50 both edit `docs/PROJECT_STATE.md`: merge #49 first, then sync #50 with `develop`
- Pending (Khoi12vn): #36 → #39 → #35 (all CHANGES_REQUESTED)
- Dependencies: #39 calendar cap uses the #48 helper (now on `develop`); #36 HIGH needs #39 right after
- Blockers: #49 push of 24ff666 + docs, then CI and Codex recheck on the new head; Codex recheck of #50 on its final head (R49-1 fixed in 398aa49, R49-2 re-fixed in 24ff666; Q49-1 and R50-2 resolved); peer review or D12 waiver, Codex review and TV1 ACCEPT for each PR

## Archive

- #48: merged 2026-11-28 as 5d03acb (head 8e7db4b, not the reviewed 7323c47) without an approving review (GOV-13). R48-1/R48-2 fixed in the merge

## Rules

- Update after each review.
- Reuse evidence only while relevant commits remain unchanged.
- Revalidate when base/head SHA or CI status changes.
- Archive closed or merged PRs.
- Never expose secrets.
- Never approve or merge without explicit authorization.
