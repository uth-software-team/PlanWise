# CLAUDE.md — Frontend (Code/frontend)

General rules (approval workflow, git, security, source of truth) are in the root `CLAUDE.md`. This file adds frontend rules.

## Status
**Scaffolded (FE-00) + router/layout/auth guard (FE-01) + API client, 401 handling and OpenAPI types (FE-02) + login/register/profile (FE-AU) + groups list/create/detail/members, transfer, leave, remove (FE-GR-01a/01b).** Vite + React + strict TypeScript + Tailwind CSS v4 + shadcn/ui + ESLint + Prettier + React Router.
Base UI on the approved API contract, not guesses.

## Runtime
- Node: version in `.nvmrc` (single source; CI reads it). npm: `packageManager` in `package.json`.
- Commit `package-lock.json`; CI installs with `npm ci`.

## Stack
React, TypeScript (strict), Vite, Tailwind CSS, shadcn/ui, ESLint, Prettier. Responsive web app.
Don't add other UI/state libraries without approval.

## Direct dependencies (keep in sync with package.json)
- Runtime: `react`, `react-dom`, `react-router-dom`, `class-variance-authority`, `lucide-react`, `cn` (pinned 0.5.2), `radix-ui` (pinned 1.8.1).
- Dev: `vite`, `@vitejs/plugin-react`, `typescript`, `@types/react`, `@types/react-dom`, `@types/node`, `tailwindcss`, `@tailwindcss/vite`, `tw-animate-css`, `eslint`, `@eslint/js`, `typescript-eslint`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh`, `globals`, `prettier`, `eslint-config-prettier`, `openapi-typescript` (pinned 7.13.0).
- `openapi-typescript` 7.13.0 declares peer `typescript ^5.x`; `overrides` in `package.json` makes it use the project's TypeScript 6 (approved by TV1 for FE-02). Re-check `npm run gen:api` + `npm run build` when bumping either.

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
- API types: `npm run gen:api` regenerates `src/api/schema.d.ts` from `docs/api/openapi-v1.json` (generated, committed, ignored by ESLint/Prettier; never edit by hand). Response schemas in the snapshot have no `required`, so response fields are optional in these types.
- API client checks: `npm run check:api-client` (T1–T15, fake fetch, no backend; T10 field errors, T11 safe redirect, T12 logout, T13 current-user store, T14–T15 group detail store); `npm run check:api-client -- --base http://localhost:5173/api/v1` with `PLANWISE_CHECK_TOKEN` + `PLANWISE_CHECK_EMAIL` runs I2–I3 against a running backend.

## API client (FE-02)
- `src/lib/apiClient.ts`: `apiRequest<T>(path, init)` with base `VITE_API_BASE_URL` (default `/api/v1`); unwraps `ApiResponse.data`, throws `ApiError{status, code, conflicts, fieldErrors}`. `fieldErrors` is read only for 400 when `data` is a flat `{field: message}` map (INVALID_INPUT, INVALID_TIME_RANGE); otherwise `{}`.
- Dev proxy (`vite.config.ts`): `/api` → `http://localhost:8080`, path kept (`/api/v1/...`), so the default base works with `npm run dev`.
- A 401 for the token the request sent clears the session (`clearToken()`), checked before the body is read; the guards then go to `/login?redirect=<path>`. A late 401 for a replaced token is ignored; 403 keeps the session.

## Routes & auth guard (FE-01)
- Router: `src/app/router.tsx` (`createBrowserRouter`). Menu config: `src/app/nav.ts`. Layouts: `src/app/layouts/`. Guards and generic pages: `src/routes/`.
- Public only: `/login`, `/register` (`features/auth`). `PublicOnlyRoute` is the only place that decides where a signed-in user goes: `resolveRedirect(?redirect=)` from `src/lib/safe-redirect.ts` (single leading `/`, same origin after URL normalisation, not `/login`/`/register`; else `/tasks`), with `replace`. Forms only call `setToken`; never navigate to `?redirect=` anywhere else (SA-13).
- Register: POST /auth/register returns the user without a token (201), so the page logs in right after; if that fails, it goes to `/login` with the same query and state `{ registered, email }`. Password: ≥ 8 characters and ≤ 72 bytes UTF-8 (backend limits).
- Protected (no token → `/login?redirect=<path>`): `/` → `/tasks` (default), `/overview`, `/tasks`, `/schedule`, `/groups`, `/ai`, `/notifications`, `/settings`.
- `/overview` and `/notifications` are P1 placeholders: their routes exist but they are hidden from MVP navigation (`inNav: false` in `nav.ts`), and the header has no notification bell. Routes come from `APP_PAGES`; the sidebar uses `MAIN_NAV`/`SECONDARY_NAV` (only `inNav: true`). Never build routes from the nav lists.
- `*` → 404 page.
- Token (CG-04): `src/lib/auth-token.ts`, key `planwise.accessToken` in `localStorage`. Always change it via `setToken`/`clearToken`; guards use `useHasToken()` and react to changes in this tab and other tabs (`storage` event). Guard checks presence only; an expired or invalid token is cleared by the API client on its first 401 (FE-02). It is the only token store: `apiClient` reads it through `getAuthToken`/`setAuthToken`. The old key `planwise_token` is deleted and never read. Logout = `signOut()` from `features/auth/session.ts` (menu and Settings): `clearToken()` first (works offline), then a best-effort `POST /auth/logout` with the old token sent explicitly; `ProtectedRoute` then goes to `/login?redirect=<current page>`.
- Current user: `useCurrentUser()` (`features/auth/session.ts`) is bound to the stored token; a token change re-fetches `/users/me`, and GET/PUT results for an older token are dropped. After `PUT /users/me`, call `setCurrentUser(user, tokenSentWithPut)` so the user menu updates.
- Feature tasks replace a placeholder by pointing its route at `features/<module>` pages; keep paths stable.
- Detail routes under a menu path are added next to the `APP_PAGES` routes (e.g. `/groups/:groupId`); `findNavItem` keeps the parent item active.

## Groups (FE-GR-01a/01b)
- `features/groups`: `/groups` (my groups, "Tạo nhóm") and `/groups/:groupId` (info, members in backend order, "add member by email" for the LEADER only, shown once `useCurrentUser()` is ready).
- `groupDetailStore.ts` (rules B2): each request carries `{token, groupId, epoch}`; the epoch changes on open, token change and close, so late results of another page, group or account are dropped. GETs also carry a counter (newest wins). Writes: `busy` while a write and its reload are in flight; 403/404/409 reload the group before the error is shown; a result for an old context returns `stale` and changes nothing.
- Dialogs use `features/groups/components/GroupDialog.tsx` (Radix Dialog: focus trap, Esc, focus return). `components/ui/dialog.tsx` has none of these; don't use it for new dialogs.
- GET 403/404: no group data stays on screen, only a link back to `/groups`.
- Every write goes through `runWrite` in the store (one rule set). Transfer applies the returned `GroupResponse` (no extra GET). Leave calls `onLeft` (navigate to `/groups`) right after the 204, with nothing awaited in between; a leave that ends after the page, group or account changed never navigates.
- D16 UI: the LEADER sees "Chuyển quyền" / "Xóa" on other members; everyone sees "Rời nhóm". The leave dialog has three variants from the loaded group (MEMBER; LEADER naming `successorUserId`, sent as `expectedSuccessorUserId`; LEADER without successor, no confirm). On STALE_VERSION/DATA_CONFLICT the group is reloaded and the dialog stays open with the new variant. Never compute the successor in the FE.
- `GroupDialog` returns focus to the opener, or to `focusFallback` (the "Thành viên" heading) when the opener is gone. The `destructive` button variant's focus ring is below 3:1: add `focus-visible:ring-ring` when using it.

## Design & accessibility
- Team design (`PlanWise Document/PlanWise AI Web Design/`) is the source of truth. Theme tokens (indigo) live in `src/index.css`; use semantic classes (`bg-primary`, `text-muted-foreground`, …), not raw colors, inside the app shell.
- Contrast: text ≥ 4.5:1, focus ring and UI boundaries ≥ 3:1. Focus rings are solid `ring-ring` (not `/50`).
- Keyboard: skip link to `#main-content`, visible `focus-visible` rings, touch targets ≥ 44px on nav and header buttons.
- Local edits to shadcn files (re-apply if a component is re-added with the CLI): `button.tsx` focus ring `ring-ring` instead of `ring-ring/50`; `input.tsx` focus ring `ring-ring` and border `border-muted-foreground/70` instead of `border-input` (boundary ≥ 3:1 on white); `dropdown-menu.tsx` `DropdownMenuItem` adds `focus-visible:outline-2 focus-visible:outline-solid focus-visible:-outline-offset-2 focus-visible:outline-ring` (inset keyboard focus ring).
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
