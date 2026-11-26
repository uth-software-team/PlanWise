# CLAUDE.md — Frontend (Code/frontend)

General rules (approval workflow, git, security, source of truth) are in the root `CLAUDE.md`. This file adds frontend rules.

## Status
**Scaffolded (FE-00).** Vite + React + strict TypeScript + Tailwind CSS v4 + shadcn/ui + ESLint + Prettier. Routing comes in FE-01; OpenAPI type generation in FE-02.
Base UI on the approved API contract, not guesses.

## Runtime
- Node: version in `.nvmrc` (single source; CI reads it). npm: `packageManager` in `package.json`.
- Commit `package-lock.json`; CI installs with `npm ci`.

## Stack
React, TypeScript (strict), Vite, Tailwind CSS, shadcn/ui, ESLint, Prettier. Responsive web app.
Don't add other UI/state libraries without approval.

## Direct dependencies (keep in sync with package.json)
- Runtime: `react`, `react-dom`, `class-variance-authority`, `lucide-react`, `cn` (pinned 0.5.2), `radix-ui` (pinned 1.8.1).
- Dev: `vite`, `@vitejs/plugin-react`, `typescript`, `@types/react`, `@types/react-dom`, `@types/node`, `tailwindcss`, `@tailwindcss/vite`, `tw-animate-css`, `eslint`, `@eslint/js`, `typescript-eslint`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh`, `globals`, `prettier`, `eslint-config-prettier`.
- Deferred: `react-router-dom` (FE-01), `openapi-typescript` (FE-02).

## Import conventions
- Class names: `import { cn, type ClassValue } from '@/lib/utils'` (re-exports the `cn` package). Don't import `clsx` or `tailwind-merge` directly.
- Primitives: `import { Slot } from 'radix-ui'` (unified package). Don't add `@radix-ui/*` packages.
- Variants: `cva` / `VariantProps` from `class-variance-authority`. Icons: `lucide-react`.
- shadcn CLI: `npx shadcn@4.23.0 add <name>` (pinned to the evaluated version). After adding a component, rewrite `from 'cn'` to `from '@/lib/utils'`, then run `npm run format` (shadcn output does not match `.prettierrc.json`).

## Commands (from package.json)
- Install: `npm ci` (use `npm install <pkg>` only for approved dependency changes)
- Dev: `npm run dev`
- Build: `npm run build` (`tsc -b` + `vite build`)
- Lint: `npm run lint` (ESLint)
- Format: `npm run format` / `npm run format:check` (Prettier)
- Add shadcn component: `npx shadcn@4.23.0 add <name>`

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
