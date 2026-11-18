# APPROVED_DECISIONS — PlanWise AI governance

Only decisions explicitly approved by TV1 (`kietit2k6`) are recorded here. Per D7, this setup records D1–D6, D8–D11 and D12 (added by setup v10) only; G1–G11, R1-x and the GitHub login mapping are **not** imported.
Strength: `SES` = command/answer from TV1 in a Claude session (authoritative in that session only); `L2` = GitHub/record/audit evidence (not independent authorization while agents can act as TV1).
The setup plan referenced below lives outside the repository; its SHA-256 identifies the exact approved text.

| ID | Decision | Approved-by | Approved-at (time + TZ) | Evidence | Strength (SES/L2) | Plan SHA-256 | Supersedes |
|---|---|---|---|---|---|---|---|
| D1 | Temporary credential model S1 with direct TV1 confirmation via Software Group; GitHub is audit only. | TV1 (`kietit2k6`) | 2026-11-18, recorded 22:26 +0700 | In-session D1–D11 confirmation and `APPROVE_SETUP docs-ai v9` | SES | `a3bfef755434196d1c98f55b641e2574c5cb6a2291e4ed4b35237c3ce4c87b47` | — |
| D2 | `AUTHORIZE_LOCAL_COMMIT` is single-use; `WORKTREE` acceptance is removed. | TV1 (`kietit2k6`) | 2026-11-18, recorded 22:26 +0700 | Same as D1 | SES | `a3bfef755434196d1c98f55b641e2574c5cb6a2291e4ed4b35237c3ce4c87b47` | — |
| D3 | Module owners fix bugs in their module; TV1 coordinates integration, cross-module regressions, unowned defects and the final E2E decision. | TV1 (`kietit2k6`) | 2026-11-18, recorded 22:26 +0700 | Same as D1 | SES | `a3bfef755434196d1c98f55b641e2574c5cb6a2291e4ed4b35237c3ce4c87b47` | — |
| D4 | Independent peer matrix (WORKFLOW §6) with fallbacks; effective only after consent of affected people and A2. | TV1 (`kietit2k6`) | 2026-11-18, recorded 22:26 +0700 | Same as D1 | SES | `a3bfef755434196d1c98f55b641e2574c5cb6a2291e4ed4b35237c3ce4c87b47` | — |
| D5 | Owners create Issues; TV1 validates ID, WBS, Track, reviewer and labels. | TV1 (`kietit2k6`) | 2026-11-18, recorded 22:26 +0700 | Same as D1 | SES | `a3bfef755434196d1c98f55b641e2574c5cb6a2291e4ed4b35237c3ce4c87b47` | — |
| D6 | TV1 checks `pg` manually before merge; it becomes required only after the three contract PRs and verification of the job/source on each base; last-push approval is deferred. | TV1 (`kietit2k6`) | 2026-11-18, recorded 22:26 +0700 | Same as D1 | SES | `a3bfef755434196d1c98f55b641e2574c5cb6a2291e4ed4b35237c3ce4c87b47` | — |
| D8 | Browser, computer-use, cloud tasks and write-capable connectors are disabled in agent sessions. | TV1 (`kietit2k6`) | 2026-11-18, recorded 22:26 +0700 | Same as D1 | SES | `a3bfef755434196d1c98f55b641e2574c5cb6a2291e4ed4b35237c3ce4c87b47` | — |
| D9 | Only TV1 merges into `develop`/`main`, one PR at a time. | TV1 (`kietit2k6`) | 2026-11-18, recorded 22:26 +0700 | Same as D1 | SES | `a3bfef755434196d1c98f55b641e2574c5cb6a2291e4ed4b35237c3ce4c87b47` | — |
| D10 | Direct authorization channel: Software Group. Store the original message link; without a permalink, store group name, sender, time with timezone and the exact command line with BLOB. | TV1 (`kietit2k6`) | 2026-11-18, recorded 22:26 +0700 | Same as D1 | SES | `a3bfef755434196d1c98f55b641e2574c5cb6a2291e4ed4b35237c3ce4c87b47` | — |
| D11 | Capacity scenario S3: TV1 adds 0.5 h/day for 12 days (21/11–02/12), 80 h total; checkpoint 26/11, and if the forecast buffer is below 4 h, TV1 decides separately on S4. | TV1 (`kietit2k6`) | 2026-11-18, recorded 22:26 +0700 | Same as D1 | SES | `a3bfef755434196d1c98f55b641e2574c5cb6a2291e4ed4b35237c3ce4c87b47` | — |
| D12 | TV1 may personally waive peer review and merge a PR opened by TV1, including the setup PR (WORKFLOW §7.1 step 6b); never delegated to an agent or teammate; PRs by TV2–TV4 keep full peer review, CI and TV1 acceptance. | TV1 (`kietit2k6`) | 2026-11-18, recorded 23:08 +0700 | In-session request 2026-11-18 and `APPROVE_SETUP docs-ai v10` | SES | `e895e0754378cc9a8d6e64bd04e47463659a788b00c30173522f704fbc9a0ec3` | — |

Note: "Approved-at" records the date of the in-session decisions and the time this file was written after `APPROVE_SETUP`; the exact minute of each in-session message was not captured.
