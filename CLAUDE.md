# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

This is an **Expense Tracker** monorepo being built **backend-first** per `docs/implementation-plan.md` and tracked task-by-task in `todo.md`. The design documents under `docs/` are the **source of truth** — treat them as the spec; if you change a decision, update the relevant doc so the docs stay authoritative.

**Build progress** (consult `todo.md` for the live state, not this paragraph):
- **Backend** is built through **Phase 2** plus **P3-1**, **P3-2**, **P3-3**, and **P3-4** — JPA `Expense` entity + Flyway migration, repository (with Testcontainers integration tests), the full CRUD `ExpenseController`/`ExpenseService`, DTOs + mapper, Bean Validation, global exception handler, `GET /api/categories`, contract tests against `docs/api-contracts.md`, the filtered/sorted/paginated `GET /api/expenses` list endpoint (current-month defaulting, `date,desc` default sort, size cap 200), `GET /api/summary` (total + count for a period, current-month defaulting, server-side aggregation), `GET /api/summary/by-category` (per-category totals/count + percent share for the donut, largest-first, percent rounded half-up to 2 decimals from exact totals), and `GET /api/summary/trend` (day/month time series, current-month defaulting, `granularity` defaulting to `day` within a single month and `month` across longer ranges, server-side aggregation with native `to_char` bucket keys). The rest of Phases 3–4 (CSV export) are **not yet built**.
- **Frontend** is **scaffolded only** (Vite + React 19 + TS, Tailwind v4, Recharts, TanStack Query, axios, date-fns, dev proxy). No feature code or test framework yet — that is Phases 5–9.

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
- `web/` — `@RestController`s (`ExpenseController`, `SummaryController`, `CategoryController`, `HealthController`), thin layers over the service. Subpackages: `web/dto/` (`ExpenseRequest`/`ExpenseResponse`, `ExpenseQuery`/`PageResponse`, `SummaryQuery`/`SummaryResponse`, `CategorySummaryResponse`, `TrendQuery`/`TrendResponse`/`Granularity`), `web/mapper/` (`ExpenseMapper`, entity↔DTO), `web/error/` (`ApiError` + `GlobalExceptionHandler`).
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
- **Frontend** (`frontend/`): `npm run dev`, `npm run build` (runs `tsc -b` then `vite build`), `npm run lint` (ESLint), `npm run format` / `npm run format:check` (Prettier). No test runner is wired yet (Vitest comes in the frontend phases).
