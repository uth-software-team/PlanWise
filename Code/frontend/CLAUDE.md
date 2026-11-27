# CLAUDE.md — Frontend (Code/frontend)

General rules (approval workflow, git, security, source of truth) are in the root `CLAUDE.md`. This file adds frontend rules.

## Status
**Scaffolded (FE-00) + router/layout/auth guard (FE-01).** Vite + React + strict TypeScript + Tailwind CSS v4 + shadcn/ui + ESLint + Prettier + React Router. OpenAPI type generation and API client come in FE-02.
Base UI on the approved API contract, not guesses.

## Runtime
- Node: version in `.nvmrc` (single source; CI reads it). npm: `packageManager` in `package.json`.
- Commit `package-lock.json`; CI installs with `npm ci`.

## Stack
React, TypeScript (strict), Vite, Tailwind CSS, shadcn/ui, ESLint, Prettier. Responsive web app.
Don't add other UI/state libraries without approval.

## Direct dependencies (keep in sync with package.json)
- Runtime: `react`, `react-dom`, `react-router-dom`, `class-variance-authority`, `lucide-react`, `cn` (pinned 0.5.2), `radix-ui` (pinned 1.8.1).
- Dev: `vite`, `@vitejs/plugin-react`, `typescript`, `@types/react`, `@types/react-dom`, `@types/node`, `tailwindcss`, `@tailwindcss/vite`, `tw-animate-css`, `eslint`, `@eslint/js`, `typescript-eslint`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh`, `globals`, `prettier`, `eslint-config-prettier`.
- Deferred: `openapi-typescript` (FE-02).

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

## Routes & auth guard (FE-01)
- Router: `src/app/router.tsx` (`createBrowserRouter`). Menu config: `src/app/nav.ts`. Layouts: `src/app/layouts/`. Guards and generic pages: `src/routes/`.
- Public only (signed-in users go to `/tasks`): `/login`, `/register` (placeholders until FE-AU).
- Protected (no token → `/login?redirect=<path>`): `/` → `/tasks` (default), `/overview`, `/tasks`, `/schedule`, `/groups`, `/ai`, `/notifications`, `/settings`.
- `/overview` and `/notifications` are P1 placeholders: their routes exist but they are hidden from MVP navigation (`inNav: false` in `nav.ts`), and the header has no notification bell. Routes come from `APP_PAGES`; the sidebar uses `MAIN_NAV`/`SECONDARY_NAV` (only `inNav: true`). Never build routes from the nav lists.
- `*` → 404 page.
- Token (CG-04): `src/lib/auth-token.ts`, key `planwise.accessToken` in `localStorage`. Always change it via `setToken`/`clearToken`; guards use `useHasToken()` and react to changes in this tab and other tabs (`storage` event). Guard checks presence only; expiry/401 handling comes in FE-02. Logout = `clearToken()` + go to `/login`.
- Feature tasks replace a placeholder by pointing its route at `features/<module>` pages; keep paths stable.

## Design & accessibility
- Team design (`PlanWise Document/PlanWise AI Web Design/`) is the source of truth. Theme tokens (indigo) live in `src/index.css`; use semantic classes (`bg-primary`, `text-muted-foreground`, …), not raw colors, inside the app shell.
- Contrast: text ≥ 4.5:1, focus ring and UI boundaries ≥ 3:1. Focus rings are solid `ring-ring` (not `/50`).
- Keyboard: skip link to `#main-content`, visible `focus-visible` rings, touch targets ≥ 44px on nav and header buttons.
- Local edits to shadcn files (re-apply if a component is re-added with the CLI): `button.tsx` focus ring `ring-ring` instead of `ring-ring/50`; `dropdown-menu.tsx` `DropdownMenuItem` adds `focus-visible:outline-2 focus-visible:outline-solid focus-visible:-outline-offset-2 focus-visible:outline-ring` (inset keyboard focus ring).
- `ui-ux-pro-max` skill: guidance only; see `.claude/skills/ui-ux-pro-max/UPSTREAM.md` for usage rules.

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
