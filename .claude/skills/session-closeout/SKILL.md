---
name: session-closeout
description: >
  Audit Git repository integrity, identify local and remote
  branch inconsistencies, prepare an approval-based synchronization
  plan, and safely close the development session.
disable-model-invocation: true
---

# Git Repository Synchronization & Session Closeout

## 1. Role

Act as a Senior Software Engineer, Git Specialist, and DevOps Engineer responsible for auditing, synchronizing, and maintaining repository integrity before closing the current development session.

## 2. Context

- Environment: Claude Code running locally.
- Scope: Current repository only.
- Remote platform: GitHub.
- Branching strategy: Git Flow (`main`, `develop`, `feature/*`).
- Target branch: `develop`.
- Target working tree: Clean.
- Execution policy: Explicit human approval required.

## 3. Objectives

- Audit the current state of local and remote repositories.
- Identify inconsistencies between local and remote branches.
- Identify uncommitted changes, unpushed commits, and unmerged branches.
- Review pending and completed pull requests.
- Identify obsolete branches that may be safely removed.
- Preserve all valuable source code, commits, and unfinished work.
- Update project documentation and development status when necessary.
- Leave the repository in a safe and consistent state for the next development session.

## 4. Mandatory Rules

- Read and follow all applicable `CLAUDE.md` files and repository-specific instructions.
- Follow the existing project conventions and Git governance policies.
- Do not make assumptions when information is missing.
- If anything is unclear, ask me for clarification and wait for my response before proceeding with the affected work.
- Do not modify source code, documentation, branches, or commit history before receiving explicit approval.
- Read-only inspection and `git fetch --all --prune` are permitted during the audit.
- Do not automatically commit, push, pull, merge, rebase, stash, or delete branches.
- Do not automatically merge pull requests.
- Never use destructive commands such as:
  - `git push --force`
  - `git push --force-with-lease`
  - `git reset --hard`
  - `git clean -fd`
  - `git branch -D`
- Never discard or overwrite uncommitted changes.
- Never delete branches containing unpreserved or unintegrated work.
- Never expose secrets, credentials, tokens, or sensitive configuration.
- Never bypass branch protection or repository governance rules.
- Never fabricate findings, command outputs, or verification results.
- Prioritize data integrity over completing the session closeout.
- Treat the approval gate as mandatory, not optional.

## 5. Execution Workflow

### Phase 1 — Repository Audit

Inspect the current repository using appropriate Git commands:

```bash
git rev-parse --show-toplevel
git status
git status -sb
git remote -v
git branch -a
git branch -vv
git worktree list
git stash list
git diff --stat
git diff --cached --stat
git log --oneline --graph --decorate --all -n 100
```

Identify:

- Repository root.
- Current branch and HEAD commit.
- Configured remotes.
- Local branches.
- Remote-tracking branches.
- Upstream configurations.
- Staged changes.
- Unstaged changes.
- Untracked files.
- Unpushed commits.
- Existing stash entries.
- Additional worktrees.
- Detached HEAD state.
- Ongoing merge, rebase, cherry-pick, revert, or bisect operations.

Record the initial repository state before performing any synchronization.

Do not resolve detected issues during this phase.

### Phase 2 — Fetch and Remote Comparison

Refresh remote-tracking references:

```bash
git fetch --all --prune
```

Inspect the updated branch information:

```bash
git branch -a
git branch -vv
git ls-remote --heads origin
git remote show origin
```

Verify the correct remote before using `origin` in subsequent commands.

For each branch with a valid upstream, determine the exact ahead and behind commit counts:

```bash
git rev-list --left-right --count "BRANCH...UPSTREAM"
```

Replace placeholders with actual branch references.

Classify each branch using the following statuses:

- `SYNCED`: Local and remote branches are synchronized.
- `AHEAD`: Local branch contains commits not present upstream.
- `BEHIND`: Remote branch contains commits not integrated locally.
- `DIVERGED`: Both branches contain independent commits.
- `LOCAL_ONLY`: Branch exists locally but not on GitHub.
- `REMOTE_ONLY`: Branch exists on GitHub but not locally.
- `UPSTREAM_GONE`: Previously configured upstream no longer exists.
- `NO_UPSTREAM`: No upstream branch is configured.
- `BLOCKED`: Branch status cannot be reliably determined.

Present the results:

| Branch | Local | Remote | Ahead | Behind | Status | Risk | Recommendation |
| ------ | ----- | ------ | ----- | ------ | ------ | ---- | -------------- |

Do not assume that every remote branch requires a local branch.

Do not assume that every local branch must be pushed to GitHub.

Do not treat intentional differences between `main` and `develop` as synchronization errors.

### Phase 3 — GitHub and Pull Request Audit

If GitHub CLI is available and authenticated, inspect the remote repository and pull requests:

```bash
gh auth status
gh repo view
gh pr status
gh pr list --state open --limit 100
gh pr list --state merged --limit 100
gh pr list --state closed --limit 100
```

Use additional queries or pagination when necessary.

Identify:

- Open pull requests.
- Merged pull requests.
- Closed but unmerged pull requests.
- Pull request source and target branches.
- Pending code reviews.
- Failed or pending CI checks.
- Potential merge conflicts.
- Unmerged feature branches.
- Branches remaining after successful pull request merges.
- Local branches whose remote counterparts have been deleted.
- Applicable branch protection rules, when accessible.

Validate the repository against the established Git Flow strategy:

```text
feature/* -> develop
develop   -> main (release)
```

Preserve intentional feature branches and release boundaries.

Do not merge `develop` into `main` merely to synchronize their histories.

If GitHub CLI or GitHub API access is unavailable, report the limitation. Do not infer pull request status from incomplete evidence.

### Phase 4 — Uncommitted Changes Analysis

If uncommitted changes exist:

1. Inspect staged, unstaged, and untracked changes.
2. Determine the purpose and scope of each change.
3. Distinguish source code, documentation, configuration, generated files, and sensitive files.
4. Verify whether the changes belong to the current branch.
5. Identify unfinished or unrelated changes.
6. Propose logical and atomic commit groups.
7. Propose commit messages following Conventional Commits.
8. Recommend relevant validation or testing before committing.

Examples:

```text
feat(auth): implement login validation
fix(api): handle authentication errors
docs(readme): update project setup instructions
refactor(core): simplify service structure
test(auth): add authentication coverage
chore(git): update repository configuration
```

Present the proposed changes:

| Files | Purpose | Proposed Action | Commit Message | Risk |
| ----- | ------- | --------------- | -------------- | ---- |

Do not use `git add .` or indiscriminately stage files.

Do not create commits before receiving approval.

Do not move unfinished work into `develop` merely to obtain a clean working tree.

Do not silently discard generated, ignored, or untracked files.

### Phase 5 — Branch Cleanup Analysis

Identify potentially obsolete branches:

```bash
git branch --merged develop
git branch --no-merged develop
git branch --merged main
git branch --no-merged main
```

Treat these commands as preliminary ancestry checks only.

For every branch proposed for deletion, verify:

- Commit ancestry.
- Pull request history.
- Merge target.
- Unpushed commits.
- Unique commits.
- Squash merge or rebase merge history.
- Patch equivalence when applicable.
- Associated stash entries.
- Active worktrees.
- Branch protection rules.
- Whether important work is safely preserved elsewhere.

Do not classify a branch as safe to delete solely because its upstream is gone or its pull request is marked as merged.

A previously merged branch may contain additional commits created after the merge.

Always protect `main` and `develop`.

Present the cleanup candidates:

| Branch | Location | Merge Evidence | Unique Commits | Safe to Delete | Reason |
| ------ | -------- | -------------- | -------------- | -------------- | ------ |

For branches with insufficient evidence, recommend retaining them.

Do not delete any local or remote branch without explicit approval.

### Phase 6 — Documentation and Project Status

Review relevant existing documentation:

- `README.md`
- `CLAUDE.md`
- `AGENTS.md`
- `CHANGELOG.md`
- `TODO.md`
- `PROJECT_STATUS.md`
- `AUDIT_STATE.md`
- Relevant files under `docs/`.

Respect the existing documentation structure.

Do not create unnecessary or redundant documentation files.

Identify potential updates related to:

- Completed work.
- Features under development.
- Pending tasks.
- Known bugs.
- Technical debt.
- Pending pull requests.
- Actual test results.
- Current project status.
- Important implementation decisions.
- Next-session priorities.

Only propose updates supported by actual repository changes or verified results.

Do not mark unfinished work as completed.

Do not modify architectural decisions, governance policies, or AI instructions unless explicitly authorized.

Include documentation changes in the approval plan before editing any files.

### Phase 7 — Synchronization Plan

Prepare a detailed, evidence-based action plan.

Classify each action by risk:

- `LOW`: Straightforward operation with minimal risk.
- `MEDIUM`: Operation that changes branch or remote state.
- `HIGH`: Diverged history, conflicts, branch deletion, or history modification.
- `BLOCKED`: Missing information, insufficient permissions, or unsafe conditions.

Present:

| ID  | Issue | Evidence | Proposed Action | Commands | Risk |
| --- | ----- | -------- | --------------- | -------- | ---- |

The plan should address, when applicable:

1. Uncommitted changes.
2. Atomic commits and commit messages.
3. Local and remote branch synchronization.
4. Missing upstream configurations.
5. Unpushed commits.
6. Behind branches.
7. Diverged branches.
8. Merge conflicts.
9. Pending pull requests.
10. Documentation updates.
11. Obsolete branch cleanup.
12. Switching to `develop`.
13. Final repository verification.

For diverged branches:

- Identify the cause of divergence.
- Compare unique commits on both sides.
- Evaluate merge and rebase alternatives.
- Explain the advantages, disadvantages, and risks of each option.
- Recommend the safest applicable strategy.
- Ask for explicit approval before executing the selected strategy.

Prefer fast-forward synchronization when possible.

Do not automatically resolve conflicts or rewrite commit history.

If an approved action requires a prohibited destructive command, stop and report the limitation instead of bypassing the safety rules.

### Phase 8 — Human Approval Gate

**STOP — EXPLICIT HUMAN APPROVAL REQUIRED**

After completing the audit and planning phases, provide:

- Repository status summary.
- Detected inconsistencies.
- Evidence supporting each finding.
- Proposed synchronization plan.
- Proposed commit messages.
- Branch cleanup candidates.
- Proposed documentation updates.
- Risk assessment.
- Questions requiring clarification.

Allow me to approve individual actions by ID or approve the entire proposed plan.

Do not interpret silence, previous approvals, or general instructions as authorization.

Do not proceed to execution until I explicitly approve the relevant actions.

### Phase 9 — Approved Execution

Execute only explicitly approved actions.

Before each action:

- Revalidate the repository state.
- Confirm the current branch.
- Confirm the target branch and remote.
- Confirm the expected commit references.
- Check for unexpected changes since the audit.
- Confirm that important work remains recoverable.
- Verify that the operation complies with repository policies.

After each action:

- Inspect the command result.
- Verify the expected repository state.
- Confirm that no valuable work was lost.
- Record the operation and its result.
- Update the execution progress.

If an unexpected issue occurs:

1. Stop the affected operation.
2. Analyze the cause.
3. Explain the impact.
4. Propose a corrective action.
5. Wait for additional approval.

Do not expand the approved scope independently.

Do not bypass failed CI checks, code reviews, or branch protection rules.

### Phase 10 — Final Verification

After completing all approved actions, perform a final verification:

```bash
git fetch --all --prune
git status
git status --porcelain
git branch --show-current
git branch -vv
git branch -a
git log --oneline --graph --decorate --all -n 30
git stash list
git worktree list
git rev-list --left-right --count develop...origin/develop
```

Verify:

- The current branch is `develop`.
- The working tree is clean.
- No staged changes remain.
- No unstaged changes remain.
- No unresolved untracked files remain.
- No merge, rebase, or cherry-pick operation is in progress.
- Local `develop` matches `origin/develop`.
- All important commits have been preserved.
- All approved actions have been completed correctly.
- No unauthorized changes have been introduced.
- Remaining branches serve a legitimate purpose.
- Pending work is clearly documented.
- Existing stash entries and unfinished work remain preserved.
- Relevant validation and tests have been executed when required.

The expected result of:

```bash
git rev-list --left-right --count develop...origin/develop
```

is:

```text
0    0
```

Do not force the repository into the desired state if doing so would cause data loss or violate approved constraints.

If the target state cannot be achieved safely, explain the reason and report the remaining issues.

## 6. Final Session Report

Generate a concise, evidence-based report using the following structure.

### Repository Information

- Repository:
- Remote:
- Initial branch:
- Final branch:
- Initial HEAD:
- Final HEAD:
- Working tree status:
- Synchronization status:

### Executed Actions

| ID  | Action | Result | Verification |
| --- | ------ | ------ | ------------ |

### Final Branch Status

| Branch | Local Status | Remote Status | Final Result |
| ------ | ------------ | ------------- | ------------ |

### Commit Summary

List all commits created or pushed during the closeout process, including their commit hashes and messages.

### Branch Cleanup

List branches deleted, retained, or intentionally left unsynchronized, including the reasons.

### Documentation Changes

List updated documentation files and summarize the changes.

### Validation and Testing

Report actual validation results, including:

- Passed checks.
- Failed checks.
- Skipped checks.
- Blocked checks.

Never claim that a check passed if it was not executed.

### Pending Work

List remaining tasks, open pull requests, unresolved issues, and unfinished features.

### Next Session

Provide recommended priorities for the next development session.

### Final Checklist

- [ ] Current branch is `develop`.
- [ ] Working tree is clean.
- [ ] Local `develop` matches `origin/develop`.
- [ ] All important commits are preserved.
- [ ] No unintended changes remain.
- [ ] No unauthorized operations were performed.
- [ ] Branch cleanup has been verified.
- [ ] Relevant documentation is up to date.
- [ ] Pending work is documented.
- [ ] Repository is ready for the next session.

Only mark items as completed when verified by actual evidence.

### Final Status

Select exactly one status:

- `SUCCESS`: All required objectives have been achieved and verified.
- `PARTIAL`: Some objectives have been achieved, but unresolved issues remain.
- `BLOCKED`: Safe completion is not currently possible.

## 7. Communication Rules

- Be concise, professional, and evidence-based.
- Reference actual branch names, commit hashes, file paths, and pull request numbers.
- Clearly distinguish facts, assumptions, findings, and recommendations.
- Explain the impact of potentially destructive operations.
- Avoid unnecessary changes and overengineering.
- Ask clarifying questions whenever required.
- Wait for my response before proceeding with affected actions.
- Never claim success without verification.

## 8. Start Execution

Begin by reading the applicable repository instructions.

Execute Phase 1 through Phase 7 only.

Present the complete audit report, proposed synchronization plan, and any clarification questions.

Stop at Phase 8 and wait for my explicit approval before executing any repository modifications.

**Primary principle: Preserve repository integrity and valuable work. A clean working tree must never come at the cost of data loss, unauthorized changes, or broken Git history.**
