# CLAUDE.md — Frontend (Code/frontend)

General rules (approval workflow, git, security, source of truth) are in the root `CLAUDE.md`. This file adds frontend rules.

## Status
**NOT started.** Do not initialize or write frontend code until the user explicitly approves the frontend phase.
Frontend starts only after the backend API contract is stable. Base UI on the approved API, not guesses.

## Stack
React, TypeScript (strict), Vite, Tailwind CSS, shadcn/ui, ESLint, Prettier. Responsive web app.
Don't add other UI/state libraries without approval.

## Commands (proposed; verify against package.json once created)
- Install: `npm install` (or the package manager chosen by the team)
- Dev: `npm run dev`
- Build: `npm run build`
- Lint: `npm run lint`

## Structure & Conventions
- Feature-based folders (e.g. `features/tasks`, `features/schedule`, `features/teams`).
- Components: PascalCase. Variables/functions: camelCase.
- Strict typing; no loose `any`. Types mirror backend DTOs.
- API client separated from UI components; base path `/api/v1`.
- Every data view handles Loading, Empty, Error, and Success states.
- ESLint + Prettier clean before proposing merge.

## UI Rules from Business Logic
- Task priority: LOW, MEDIUM, HIGH, URGENT. Status: TODO, IN_PROGRESS, COMPLETED, CANCELLED. Overdue is computed, not a status.
- Schedule: PROPOSED sessions are shown as suggestions; user must explicitly confirm. CONFIRMED sessions are protected. Show conflict info when the backend reports insufficient time.
- Assignment: PENDING / ACCEPTED / DECLINED / REVOKED, separate from task status. Declining requires a reason input.
- Privacy: leaders see only BUSY/FREE for members; never display others' private task titles, notes, or event details.
- AI: show AI output as a suggestion to review/edit before saving; on AI failure, fall back to manual input.

## Security
- No secrets or API keys in frontend code or `VITE_*` vars. Never call AI providers directly from the browser.
- Store/handle auth tokens per the approved backend auth design.
