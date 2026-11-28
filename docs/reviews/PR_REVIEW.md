# Pull Request Review State

Track active PR reviews. Keep concise and evidence-based.

## Reviews

| PR  | Base | Head SHA | CI  | Verdict | Findings |
| --- | ---- | -------- | --- | ------- | -------- |
| #48 | develop @ f6f602d | 7323c47 | 4/4 SUCCESS | BLOCKED (process: TV2 + TV4 contract review, Codex, TV1 ACCEPT) | R48-1 MEDIUM, R48-2 LOW (both fixed locally, not pushed) |

Verdict: READY / CHANGES_REQUIRED / BLOCKED / UNVERIFIED.

## Findings

| PR  | ID  | Severity | File:Line | Issue | Suggested Fix | Status |
| --- | --- | -------- | --------- | ----- | ------------- | ------ |
| #48 | R48-1 | MEDIUM | ScheduleService.java:381-387 | Edit/confirm expand availability over [min session start, max session end] with no cap; an edited session moved years ahead bypasses the 366-day rule once #36 adds `AvailabilityApi` (day loop AvailabilityService.java:111 on #36) | TV1 chose option A: `editSession` rejects 400 `INVALID_TIME_RANGE` when the post-edit batch span > 366 days (D14) | FIXED locally 2026-11-28 (uncommitted; `./gradlew test` 375/375 pass); recheck after TV1 pushes |
| #48 | R48-2 | LOW | Code/backend/CLAUDE.md:40-41 | "D12/D13 (backend)" reuse governance IDs D12/D13; D12 lists `/v1/calendar` and D13 describes #35, neither on develop yet | Renamed to D14/D15, D15 wording fixed, "effective with #39/#35" added; PR body line 26 still says D12/D13 (TV1 edits) | FIXED locally 2026-11-28 (uncommitted) |

## Merge Order

- Pending: #48 → #36 → #39 → #35 (order from PR #48 body)
- Dependencies: #39 calendar cap uses #48 helper; #36 HIGH needs #39 right after
- Blockers: #48 needs TV2 + TV4 review (01_CONTRACT_GATES.md:6, a governance D12 waiver does not cover it), Codex review, TV1 ACCEPT

## Rules

- Update after each review.
- Reuse evidence only while relevant commits remain unchanged.
- Revalidate when base/head SHA or CI status changes.
- Archive closed or merged PRs.
- Never expose secrets.
- Never approve or merge without explicit authorization.
