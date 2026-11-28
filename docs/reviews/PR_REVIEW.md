# Pull Request Review State

Track active PR reviews. Keep concise and evidence-based.

## Reviews

| PR  | Base | Head SHA | CI  | Verdict | Findings |
| --- | ---- | -------- | --- | ------- | -------- |

Verdict: READY / CHANGES_REQUIRED / BLOCKED / UNVERIFIED.

## Findings

| PR  | ID  | Severity | File:Line | Issue | Suggested Fix | Status |
| --- | --- | -------- | --------- | ----- | ------------- | ------ |

## Merge Order

- Pending: TBD
- Dependencies: TBD
- Blockers: None

## Rules

- Update after each review.
- Reuse evidence only while relevant commits remain unchanged.
- Revalidate when base/head SHA or CI status changes.
- Archive closed or merged PRs.
- Never expose secrets.
- Never approve or merge without explicit authorization.
