# TASK_TEMPLATE — PlanWise AI

Copy **Part A** to `docs/ai/tasks/<TASK_ID>/plan-v<N>.md` and **Part B** to `docs/ai/tasks/<TASK_ID>/record.md` on the task branch.
Rules: `docs/ai/WORKFLOW.md`. A plan file is immutable once submitted for review; a scope change means a new `plan-v<N+1>.md`. `record.md` is append-only and stops at the reviewed head (later audit goes to Issue/PR comments).

---

## Part A — `plan-v<N>.md`

```text
Task-ID: PW-<BE|FE|INT|TEST>-<NNN>
Issue: #<n>
WBS: <ids>
Plan-Version: v<N>
Base-SHA: <40hex>
Track: A|B
Owner: TVx
Peer: TVx (fallback TVy)
Files: <allowed paths, one per line or comma-separated>
STATUS: UNAPPROVED — PLANNING ONLY
```

### Verified findings
<!-- Evidence-based facts only: file:line, command + output. Mark ASSUMPTION / UNKNOWN separately. -->

### Scope
<!-- In scope / out of scope. -->

### Steps
<!-- Ordered implementation steps. -->

### Impact (API / DB / security / other modules)
<!-- Contract, migration, auth, cross-module calls, dependencies. "None" must be justified. -->

### Tests
<!-- Tests to add/run and the exact commands. -->

### Acceptance criteria

### Risks

### Rollback

<!-- Track B (Light): keep each section short; the whole plan stays within about 30 lines. -->

---

## Part B — `record.md` (append-only)

### Plan Review
<!-- Per plan version: BLOB, Codex findings (severity + evidence), questions, Technical Directive, recommendation (not authorization). Relayed verbatim by TV1. -->

### Authorization ref (D10)
<!-- Software Group: message link; if no permalink: group name, sender, time with timezone, and the exact one-line command with BLOB. -->

### Implementation
<!-- Approved plan version + BLOB, changed files, head SHA, notes. -->

### CI evidence
<!-- Run id, attempt, event, H, M, M parents [D, H], applicable jobs and results. -->

### Code Review
<!-- Reviewed head, peer review, Codex findings, required changes, recommendation. -->

### IA
<!-- Only when the base changes: D_old, D_new, H, H', clean-merge evidence, impact, decision. -->

### Handoff
<!-- Current state per Issue label, blockers, next action that needs authorization. -->
