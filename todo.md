# Expense Tracker — Implementation Todo

Backend-first build order: build and verify the Spring Boot API against
`docs/api-contracts.md`, then build the React frontend against the live API. Phases
are sequential milestones; check each task as it lands. Testing tasks are woven
into each phase per `docs/testing-plan.md`.

Each task has a unique ID (`<phase>-<n>`) so it can be assigned to a team
member. Add an **Owner** column note next to any task as you delegate.

---

## Phase 0 — Project Setup & Tooling

- [x] **P0-1** Create monorepo structure: `/backend` (Spring Boot), `/frontend` (Vite React), `/docs` (existing MD files)
- [ ] **P0-2** Docker Compose for local Postgres (`expense-tracker` DB + persistence volume)
- [ ] **P0-3** Scaffold backend via Spring Initializr: Web, Data JPA, Validation, PostgreSQL driver, Flyway, Testcontainers (Java 21, Spring Boot 3.x)
- [ ] **P0-4** Configure `application.yml` (datasource, JPA, Flyway) with env-var overrides
- [ ] **P0-5** Scaffold frontend via Vite (React + TS); add Tailwind, Recharts, TanStack Query, axios, date-fns
- [ ] **P0-6** Configure Vite dev proxy `/api` → backend; configure backend CORS for dev origin
- [ ] **P0-7** Tooling: ESLint + Prettier (frontend); Spotless/Checkstyle optional (backend)
- [ ] **P0-8** Basic CI (build + test both apps); README with run instructions
- [ ] **Exit:** `docker compose up` + `mvnw spring-boot:run` + `npm run dev` all start; `/api/health` returns OK and the SPA loads

---

## Phase 1 — Data Model & Persistence (Backend)

- [ ] **P1-1** `Category` enum: `FOOD, TRANSPORT, RENT, UTILITIES, GROCERIES, ENTERTAINMENT, HEALTH, SHOPPING, OTHER`
- [ ] **P1-2** `Expense` JPA entity: UUID id, `BigDecimal` amount (`NUMERIC(12,2)`), `LocalDate` date, category, created/updated timestamps
- [ ] **P1-3** Flyway `V1__init.sql`: `expenses` table + indexes on `date` and `category`
- [ ] **P1-4** `ExpenseRepository` (Spring Data JPA) with derived/custom filter queries
- [ ] **P1-5** Repository integration tests with Testcontainers (real Postgres)
- [ ] **Exit:** entity persists and queries pass against real Postgres in tests

---

## Phase 2 — CRUD API

- [ ] **P2-1** DTOs: `ExpenseRequest`, `ExpenseResponse` + entity↔DTO mapping
- [ ] **P2-2** `ExpenseService` — create/read/update/delete business logic
- [ ] **P2-3** `ExpenseController` — `POST /api/expenses`, `GET /api/expenses/{id}`, `PUT /api/expenses/{id}`, `DELETE /api/expenses/{id}`
- [ ] **P2-4** Bean Validation (amount > 0, required date/category) + global exception handler with uniform error shape (`timestamp, status, error, message, path, fieldErrors[]`)
- [ ] **P2-5** `GET /api/categories` reference endpoint (returns fixed enum set)
- [ ] **P2-6** Web-layer tests (`@WebMvcTest`) + service unit tests (Mockito)
- [ ] **P2-7** Contract tests (MockMvc) against `docs/api-contracts.md`: status codes, body shapes, validation/error JSON
- [ ] **Exit:** CRUD endpoints match `docs/api-contracts.md`; validation + error responses verified

---

## Phase 3 — List, Filter, Pagination & Summaries

- [ ] **P3-1** `GET /api/expenses` with `from`, `to`, `category`, `minAmount`, `maxAmount`, `sort`, `page`, `size`; paginated response (defaults to current month)
- [ ] **P3-2** `GET /api/summary` — total + count for a period (defaults to current month)
- [ ] **P3-3** `GET /api/summary/by-category` — per-category totals/percent (donut)
- [ ] **P3-4** `GET /api/summary/trend` — time series with `granularity` (day/month)
- [ ] **P3-5** Aggregations via JPQL/native queries; verified for money precision
- [ ] **P3-6** Integration tests for filtering, pagination, and each summary endpoint (empty month, single category, mixed)
- [ ] **Exit:** all read/aggregation endpoints return correct, tested data

---

## Phase 4 — CSV Export

- [ ] **P4-1** `GET /api/expenses/export` — same filters as list, no pagination
- [ ] **P4-2** Stream CSV with correct headers (`text/csv`, `Content-Disposition`)
- [ ] **P4-3** Test content + headers; INR amounts formatted with 2 decimals, no scientific notation
- [ ] **Exit:** export downloads a correct CSV for the active filter set

---

## Phase 5 — Frontend Foundation

- [ ] **P5-1** App shell, layout, Tailwind theme matching wireframes (clean/minimal)
- [ ] **P5-2** Typed API client (`Expense`, request/response types) + TanStack Query hooks (`useExpenses`, `useSummary`, etc.)
- [ ] **P5-3** Month selector state (defaults to current month) feeding all queries
- [ ] **P5-4** Loading skeletons + error states (per `docs/wireframes/loading.html`)
- [ ] **P5-5** Hook tests with MSW (loading/success/error, cache invalidation on mutation)
- [ ] **Exit:** app loads, calls the real API, shows loading/error states

---

## Phase 6 — Dashboard & Charts

- [ ] **P6-1** "This Month" total card
- [ ] **P6-2** Category donut (Recharts) from `/summary/by-category` + legend
- [ ] **P6-3** Spending trend (Recharts bar/line) from `/summary/trend`
- [ ] **P6-4** Empty state (no expenses) per `docs/wireframes/empty.html`
- [ ] **P6-5** Component tests: charts render summary data + empty/zero state
- [ ] **Exit:** dashboard renders real data and matches the dashboard wireframe

---

## Phase 7 — Expense List, Add/Edit Modal, Delete

- [ ] **P7-1** Expense table (date, category pill, amount, actions)
- [ ] **P7-2** Add/Edit modal form (amount, date, category) with client validation (amount > 0, required fields)
- [ ] **P7-3** Create/update via API + cache invalidation/refresh
- [ ] **P7-4** Delete with confirm prompt
- [ ] **P7-5** Component tests: modal validation + edit pre-fill; table rows/actions/empty state
- [ ] **Exit:** add/edit/delete work end to end and the dashboard updates live

---

## Phase 8 — Filters, Search & CSV Download

- [ ] **P8-1** Filter bar: date range, category, amount range, search — bound to list query params
- [ ] **P8-2** Filters also drive charts/summary for the selected scope
- [ ] **P8-3** Export CSV button hitting the export endpoint with current filters
- [ ] **P8-4** Component tests: changing filters updates query params
- [ ] **Exit:** filtering/search refine list + charts; CSV downloads the filtered set

---

## Phase 9 — Polish, Testing & Hardening

- [ ] **P9-1** Responsive/visual polish vs. wireframes; accessibility pass (labels, focus, contrast)
- [ ] **P9-2** Frontend component tests (Vitest + Testing Library) complete for form, list, charts, filters
- [ ] **P9-3** Formatting util tests: INR display, date formatting/grouping
- [ ] **P9-4** Playwright E2E for key flows: add → dashboard updates; edit; delete + confirm; filter & search; month navigation + empty state; export CSV; loading/error + retry
- [ ] **P9-5** Money/decimal precision suite (explicit): storage round-trip, aggregation/percentages, `0.01` accepted / `0` & negative rejected, CSV 2-decimal, FE display
- [ ] **P9-6** CI gates green: backend `mvnw verify` (unit + integration + JaCoCo); frontend `vitest run --coverage` + lint; Playwright suite
- [ ] **P9-7** Update docs/README; finalize Docker Compose for full local run
- [ ] **Exit:** v1 feature-complete, tested, runnable locally via documented steps

---

## Cross-Cutting Conventions (must hold everywhere money/API is touched)

- [ ] **X-1** Money is exact decimal end-to-end (`BigDecimal` / `NUMERIC(12,2)`); never floats. INR, 2 decimals, amount `> 0`
- [ ] **X-2** API base path `/api`; JSON everywhere except CSV export; IDs are UUID strings
- [ ] **X-3** Dates `YYYY-MM-DD`; timestamps ISO-8601 UTC
- [ ] **X-4** Categories are the fixed enum — no custom categories in v1
- [ ] **X-5** Uniform error shape via global exception handler
- [ ] **X-6** List/summary/export default to current month when `from`/`to` omitted; summary endpoints return ready-to-chart JSON computed server-side

## Out of Scope (v1)

Auth/multi-user, recurring expenses, attachments, CSV import, multi-currency,
budgets/limits, load/stress/security testing, cross-browser matrix.
