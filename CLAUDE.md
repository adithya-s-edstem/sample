# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

This is an **Expense Tracker** monorepo being built **backend-first** per `docs/implementation-plan.md` and tracked task-by-task in `todo.md`. The design documents under `docs/` are the **source of truth** — treat them as the spec; if you change a decision, update the relevant doc so the docs stay authoritative.

**Build progress** (consult `todo.md` for the live state, not this paragraph):
- **Backend** is built through **Phase 2**, all of **Phase 3** (**P3-1**…**P3-6**), and all of **Phase 4** (**P4-1**…**P4-3**, CSV export) — JPA `Expense` entity + Flyway migration, repository (with Testcontainers integration tests), the full CRUD `ExpenseController`/`ExpenseService`, DTOs + mapper, Bean Validation, global exception handler, `GET /api/categories`, contract tests against `docs/api-contracts.md`, the filtered/sorted/paginated `GET /api/expenses` list endpoint (current-month defaulting, `date,desc` default sort, size cap 200), `GET /api/summary` (total + count for a period, current-month defaulting, server-side aggregation), `GET /api/summary/by-category` (per-category totals/count + percent share for the donut, largest-first, percent rounded half-up to 2 decimals from exact totals), `GET /api/summary/trend` (day/month time series, current-month defaulting, `granularity` defaulting to `day` within a single month and `month` across longer ranges, server-side aggregation with native `to_char` bucket keys), and `GET /api/expenses/export` (CSV download — same filters as the list, current-month defaulting, `date,desc` default sort, **no pagination**, `id,date,category,amount` rows with 2-decimal amounts, `text/csv; charset=UTF-8` + `Content-Disposition` attachment; **streamed** via `StreamingResponseBody` from a cursor-backed `ExpenseRepository.streamAll` so the result set is never buffered in memory), with full-stack export contract tests (P4-3) pinning the `text/csv` + `Content-Disposition` headers, the exact CSV body for the active filter set, and 2-decimal/no-scientific-notation amounts through real Postgres. Phase 4 is complete; the remaining work is the React frontend (Phases 5–9).
- **Frontend** is scaffolded (Vite + React 19 + TS, Tailwind v4, Recharts, TanStack Query, axios, date-fns, dev proxy) plus all of **Phase 5** (**P5-1**…**P5-5**). **P5-1**: the single-page **app shell** — wireframe-matching Tailwind `@theme` tokens (`src/index.css`) and static layout region components under `src/components/` (`layout/Header`, `layout/Card`, `dashboard/SummaryRow`, `dashboard/TrendSection`, `dashboard/FilterBar`, `dashboard/ExpenseListSection`) composed in `App.tsx`. **P5-2**: the **typed API layer** under `src/api/` — wire types mirroring the backend DTOs (`types.ts`), a shared axios instance on the `/api` base path with uniform-error helpers (`client.ts`), typed endpoint wrappers for CRUD/list/summaries/categories/CSV-export-URL (`expenses.ts`), and a hierarchical query-key factory (`queryKeys.ts`) — and the **TanStack Query hooks** under `src/hooks/` (`useExpenses`/`useExpense`, `useSummary`/`useSummaryByCategory`/`useSummaryTrend`, `useCategories`, and the `useCreateExpense`/`useUpdateExpense`/`useDeleteExpense` mutations that invalidate the `expenses` + `summary` cache roots on success). **P5-3**: **selected-month state** — pure month helpers (`src/lib/month.ts`: current month, `MMMM yyyy` label, inclusive `YYYY-MM-DD` `from`/`to` range matching the backend current-month default, prev/next) and a `MonthProvider`/`useMonth` context (`src/context/MonthContext.tsx` provider + `src/context/monthContext.ts` context/hook) mounted in `main.tsx`, defaulting to the current month. The header drives prev/next month navigation and shows the label; `SummaryRow`/`TrendSection`/`ExpenseListSection` read the month `range` from `useMonth` and pass it as `from`/`to` to their summary/trend/list queries, so the whole page is scoped to the selected month. **P5-4**: each data-backed region (`SummaryRow`, `TrendSection`, `ExpenseListSection`) renders wireframe-matching loading skeletons while its query is pending and a graceful `ErrorState` + Retry on failure (`layout/Skeleton`, `layout/ErrorState`). **P5-5**: the **frontend test stack** is wired — Vitest (jsdom, `threads` pool) + Testing Library + MSW, configured via the `test` block in `vite.config.ts` with a global setup at `src/test/setup.ts` (jest-dom matchers, MSW server lifecycle). Test scaffolding lives under `src/test/` (`msw/server.ts`, `msw/handlers.ts` mirroring `api-contracts.md` happy paths, and `utils.tsx` with a per-test QueryClient + `renderHookWithClient`); the hook tests sit beside the hooks (`src/hooks/*.test.tsx`) and cover loading/success/error and cache-invalidation-on-mutation. Run with `npm run test` / `npm run test:coverage` (also a CI gate; `testTimeout` is raised in `vite.config.ts` so the heavy jsdom suite stays stable under parallelism). **Phase 6** (dashboard & charts) and **Phase 7** (expense list, add/edit modal, delete) are now complete: the regions render real data — the "This Month" total card, the Recharts category donut (`dashboard/CategoryDonut`) and spending-trend chart (`dashboard/TrendChart`) with the no-spending empty state (`dashboard/EmptyState`), and the expense table (`dashboard/ExpenseRow` rows with date / category pill / amount / edit+delete actions). The Add/Edit flow is a presentational modal (`dashboard/ExpenseModal`: client validation — amount `> 0`, required date/category — and edit pre-fill) wrapped by a stateful container (`dashboard/ExpenseFormModal`) that runs the create/update mutation and surfaces server errors inline; delete-with-confirm is `dashboard/ConfirmDialog` + `dashboard/DeleteExpenseDialog`. `App.tsx` owns the modal/delete state so the header, empty-state CTA, and each row's actions drive the same modal, and successful writes invalidate the list + summary caches so the dashboard updates live. Component tests sit beside each component (`*.test.tsx`) and an `App.test.tsx` integration test exercises the Phase 7 exit end to end (add/edit/delete against a stateful MSW backend, asserting the live dashboard refresh). Phase 8 (filters/search/CSV download) and Phase 9 (polish/E2E/hardening) remain.

The monorepo layout (established in **P0-1**) is:
- `docs/` — design/spec documents (the source of truth) and wireframe mockups.
- `backend/` — Spring Boot 3.5 / Java 21 REST API.
- `frontend/` — Vite React SPA.
- `CLAUDE.md`, `todo.md`, `docker-compose.yml` — kept at the repo root.

The docs were authored in a deliberate order (see `docs/plan.md`).

## Document map

Read these to understand the intended system before writing any code:

- `docs/requirements.md` — product scope, data model, in/out-of-scope features. **Note:** it originally said "local-only / no backend"; this was superseded — see the architecture doc.
- `docs/technical-architecture.md` — the binding stack decision and DB schema. **Supersedes** the storage decision in `docs/requirements.md`: data is persisted server-side in PostgreSQL via a Spring Boot API.
- `docs/solution.md` — UX/layout: single scrollable page, current-month focus, add/edit modal.
- `docs/api-contracts.md` — REST endpoints, DTOs, validation rules, error shape. Implementations must match this exactly.
- `docs/wireframes.md` + `docs/wireframes/` — rendered HTML/CSS mockups (`dashboard.html`, `modal.html`, `empty.html`, `loading.html`, shared `styles.css`). These are **design references only**, not the production frontend (which will be React/Tailwind/Recharts).
- `docs/implementation-plan.md` — phased, backend-first build plan (Phase 0 setup → Phase 9 hardening) with the intended monorepo layout (`/backend`, `/frontend`, `/docs`).
- `docs/testing-plan.md` — test pyramid strategy and CI gates.

## Architecture

React SPA (Vite + TypeScript + Tailwind + Recharts + TanStack Query) → Spring Boot 3.5 REST API (Java 21, Spring Web + Data JPA, Bean Validation, Flyway) → PostgreSQL. Single source of truth is the DB; the frontend only talks to the API. Single-user, **no auth in v1**. Docker Compose at the repo root runs local Postgres.

**Backend package layout** (`backend/src/main/java/com/expensetracker/`) is a conventional layered structure — keep new code in the matching layer:
- `domain/` — `Category` enum and the `Expense` JPA entity.
- `repository/` — `ExpenseRepository` (CRUD + filter specs + the `summarize` / `summarizeByCategory` / `summarizeTrendByDay` / `summarizeTrendByMonth` aggregation queries; the trend queries are native Postgres so `to_char` formats the bucket key) + `ExpenseSpecifications` (JPA Criteria specs for dynamic filtering); `repository/projection/` holds Spring Data projections (`SummaryProjection`, `CategorySummaryProjection`, `TrendProjection`) for aggregation reads.
- `service/` — `ExpenseService` business logic; `ExpenseNotFoundException` (thrown on missing id, mapped to 404 by the global handler).
- `web/` — `@RestController`s (`ExpenseController`, `SummaryController`, `CategoryController`, `HealthController`), thin layers over the service. Subpackages: `web/dto/` (`ExpenseRequest`/`ExpenseResponse`, `ExpenseQuery`/`PageResponse`, `SummaryQuery`/`SummaryResponse`, `CategorySummaryResponse`, `TrendQuery`/`TrendResponse`/`Granularity`), `web/mapper/` (`ExpenseMapper`, entity↔DTO), `web/error/` (`ApiError` + `GlobalExceptionHandler`), `web/csv/` (`ExpenseCsvWriter`, renders the export rows to RFC 4180 CSV with 2-decimal amounts).
- `config/` — `WebCorsConfig` (CORS for the Vite dev origin).

**Schema is Flyway-owned.** Migrations live in `backend/src/main/resources/db/migration/` (`V1__init.sql`). Hibernate runs `ddl-auto=validate` and must **never** mutate the schema — change the DB only by adding a new `V__` migration whose types match the entity. The DB enforces money rules too (`amount NUMERIC(12,2)` with a `CHECK (amount > 0)`); timestamps are `TIMESTAMPTZ` stored in UTC.

**Frontend dev proxy:** Vite proxies `/api` → `http://localhost:8080` (override `VITE_API_PROXY_TARGET`), so the SPA uses same-origin relative URLs and avoids browser CORS in dev. Tailwind v4 is wired via the `@tailwindcss/vite` plugin (no `tailwind.config.js`).

## Conventions that must hold across the codebase

These are cross-cutting rules from the specs — get them right wherever money or the API is touched:

- **Money is exact decimal end-to-end.** Use `BigDecimal` / `NUMERIC(12,2)` server-side; never floats. Amounts are INR, two decimals, must be `> 0`. There is an explicit money-precision test suite requirement.
- **API base path is `/api`**; JSON everywhere except the CSV export endpoint. IDs are UUID strings. Dates are `YYYY-MM-DD`; timestamps are ISO-8601 UTC.
- **Categories are a fixed enum** (`FOOD, TRANSPORT, RENT, UTILITIES, GROCERIES, ENTERTAINMENT, HEALTH, SHOPPING, OTHER`) — no custom categories in v1.
- **Errors use one uniform shape** (`timestamp, status, error, message, path, fieldErrors[]`) via a global exception handler.
- **List/summary/export endpoints default to the current month** when `from`/`to` are omitted. Summary endpoints (`/summary`, `/summary/by-category`, `/summary/trend`) return ready-to-chart JSON computed server-side.
- **Backend-first build order**: build and test the API against `docs/api-contracts.md`, then build the frontend against the live API.

## Build/run/test

- **DB:** `docker compose up` (local Postgres; matches the connection defaults baked into `application.yml`).
- **Backend** (`backend/`, use the `mvnw` wrapper):
  - `./mvnw spring-boot:run` — start the API on :8080.
  - `./mvnw verify` — full build: unit + integration tests **and** the Spotless format check (binds to `verify`). **Testcontainers integration tests need Docker running.**
  - `./mvnw spotless:apply` — auto-fix formatting (Palantir Java Format, LF line endings). Run this before committing; otherwise `verify` fails on formatting.
  - Single test: `./mvnw test -Dtest=ExpenseServiceTest` (or `-Dtest=ClassName#method`).
- **Frontend** (`frontend/`): `npm run dev`, `npm run build` (runs `tsc -b` then `vite build`), `npm run lint` (ESLint), `npm run format` / `npm run format:check` (Prettier), `npm run test` (Vitest run; `npm run test:watch` for the inner loop, `npm run test:coverage` for v8 coverage). Vitest runs in jsdom with Testing Library + MSW; tests live beside the code as `*.test.tsx` and in `src/test/`.
