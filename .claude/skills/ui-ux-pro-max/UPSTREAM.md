# Upstream provenance — ui-ux-pro-max

| Field | Value |
|---|---|
| Repository | https://github.com/nextlevelbuilder/ui-ux-pro-max-skill |
| Release | `v2.15.0` |
| Commit | `a38d04c3d5c298c851dbe5e6ee1965ee3de42cb5` (2026-08-13) |
| Source path | `.claude/skills/ui-ux-pro-max/` |
| License | MIT — copied from upstream root `LICENSE` into `LICENSE` here |
| Vendored | 2026-11-21, by TV1 decision (FE-01 design guidance) |
| Install method | Files downloaded individually at the pinned commit; no upstream CLI, installer or script was executed |

## Verification

Every file was checked with `git hash-object --no-filters` against the Git blob ID in the upstream tree at the pinned commit:
71 files (70 from `.claude/skills/ui-ux-pro-max/` + root `LICENSE`), all mode `100644`, no symlinks. All 71 matched before the local change below.

## Local changes

Only one file differs from upstream:

| File | Upstream blob | Local blob | Change |
|---|---|---|---|
| `SKILL.md` | `41f8e2fd7f8c568228d0b55186ebe3f7b4007377` | `cb3835bce867314b395be96855308e1652e7c4f6` | 11 lines: `"${CLAUDE_PLUGIN_ROOT}/.claude/skills/ui-ux-pro-max/scripts/search.py"` → `".claude/skills/ui-ux-pro-max/scripts/search.py"` |

Reason: `CLAUDE_PLUGIN_ROOT` is only set for marketplace plugin installs. In this project-local copy it is unset, so the original path resolves to `/.claude/...` and fails. The new path is relative to the repository root; run the script from the repo root.

`LICENSE` and this `UPSTREAM.md` are additions, not upstream files in this directory.

## Usage rules (PlanWise)

- The team design in `PlanWise Document/PlanWise AI Web Design/` is the source of truth; this skill gives UX, accessibility and pre-delivery guidance only.
- Run from the repo root with `python3 -B` (no `__pycache__` in the tree), e.g.
  `python3 -B .claude/skills/ui-ux-pro-max/scripts/search.py "keyboard focus modal" --domain ux`
- Do not use `--persist` (it writes a `design-system/` folder) unless TV1 approves.
- Queries are generic design terms only; never include private project data.
- `scripts/tests/` is kept as upstream but is not run here: several tests call scripts that exist only at the upstream repository root.

## Updating

Pick a new upstream tag, re-download the same path at that commit, re-verify every blob ID, re-apply the `SKILL.md` path change, and update this file. Changes need TV1 approval.
