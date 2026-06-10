# Expense Tracker — Implementation Plan

_Last updated: 2026-06-10_

## Strategy

**Backend-first.** Build the Spring Boot API + PostgreSQL schema with tests so
the API contract (`api-contracts.md`) is real and verified, then build the
React frontend against the working API. Phases are ordered milestones; each has
a task checklist. Setup is included as Phase 0.

Legend: `[ ]` todo.

---

## Phase 0 — Project Setup & Tooling

**Goal:** repos, local infra, and scaffolding ready; "hello world" runs end to end.

- [ ] Create repo structure (monorepo): `/backend` (Spring Boot), `/frontend` (Vite React), `/docs` (these MD files).
- [ ] **Docker Compose** for local Postgres (`expense-tracker` DB, volume for persistence).
- [ ] Scaffold backend with Spring Initializr: Web, Data JPA, Validation, PostgreSQL driver, Flyway, Testcontainers.
- [ ] Configure `application.yml` (datasource, JPA, Flyway) + env-var overrides.
- [ ] Scaffold frontend with Vite (React + TS); add Tailwind, Recharts, TanStack Query, axios, date-fns.
- [ ] Configure Vite dev proxy `/api` → backend; configure backend CORS for the dev origin.
- [ ] Tooling: ESLint + Prettier (frontend), Checkstyle/Spotless optional (backend).
- [ ] Basic CI (build + test both apps); README with run instructions.

**Exit:** `docker compose up` (Postgres) + `mvnw spring-boot:run` + `npm run dev` all start; a trivial `/api/health` returns OK and the SPA loads.

---

## Phase 1 — Data Model & Persistence (Backend)

**Goal:** `Expense` persisted in Postgres with migrations.

- [ ] `Category` enum (FOOD … OTHER).
- [ ] `Expense` JPA entity (UUID id, `BigDecimal` amount, `LocalDate` date, category, timestamps).
- [ ] Flyway `V1__init.sql` — `expenses` table + indexes on `date` and `category`.
- [ ] `ExpenseRepository` (Spring Data JPA) + derived/custom queries for filtering.
- [ ] Repository integration tests with **Testcontainers** (real Postgres).

**Exit:** entity persists and queries pass against a real Postgres in tests.

---

## Phase 2 — CRUD API

**Goal:** create/read/update/delete expenses over REST.

- [ ] DTOs: `ExpenseRequest`, `ExpenseResponse` + mapping.
- [ ] `ExpenseService` — CRUD business logic.
- [ ] `ExpenseController` — `POST`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`.
- [ ] Bean Validation (amount > 0, required date/category) + global exception handler with uniform error shape.
- [ ] `GET /api/categories` reference endpoint.
- [ ] Controller/web-layer tests (`@WebMvcTest`) + service unit tests.

**Exit:** CRUD endpoints match `api-contracts.md`; validation + error responses verified.

---

## Phase 3 — List, Filter, Pagination & Summaries

**Goal:** querying and aggregation endpoints powering the dashboard.

- [ ] `GET /api/expenses` with `from`, `to`, `category`, `minAmount`, `maxAmount`, `sort`, `page`, `size`; paginated response.
- [ ] `GET /api/summary` — total + count for a period (defaults to current month).
- [ ] `GET /api/summary/by-category` — per-category totals/percent (donut).
- [ ] `GET /api/summary/trend` — time series with `granularity` (day/month).
- [ ] Aggregation implemented via JPA/JPQL or native queries; verified for money precision.
- [ ] Integration tests for filtering, pagination, and each summary endpoint.

**Exit:** all read/aggregation endpoints return correct, tested data.

---

## Phase 4 — CSV Export

**Goal:** downloadable CSV honoring filters.

- [ ] `GET /api/expenses/export` — same filters as list, no pagination.
- [ ] Stream CSV with correct headers (`text/csv`, `Content-Disposition`).
- [ ] Test content + headers; verify INR amounts formatted with 2 decimals.

**Exit:** export downloads a correct CSV for the active filter set.

---

## Phase 5 — Frontend Foundation

**Goal:** app shell + API layer wired to the live backend.

- [ ] App shell, layout, Tailwind theme matching wireframes (clean/minimal).
- [ ] Typed API client (`Expense`, requests, responses) + TanStack Query hooks (`useExpenses`, `useSummary`, etc.).
- [ ] Month selector state (defaults to current month) feeding all queries.
- [ ] Loading skeletons + error states (per `wireframes/loading.html`).

**Exit:** app loads, calls the real API, and shows loading/error states.

---

## Phase 6 — Dashboard & Charts

**Goal:** the single-page dashboard.

- [ ] "This Month" total card.
- [ ] Category donut (Recharts) from `/summary/by-category` + legend.
- [ ] Spending trend (Recharts bar/line) from `/summary/trend`.
- [ ] Empty state (no expenses) per `wireframes/empty.html`.

**Exit:** dashboard renders real data; matches the dashboard wireframe.

---

## Phase 7 — Expense List, Add/Edit Modal, Delete

**Goal:** full expense management UI.

- [ ] Expense table (date, category pill, amount, actions).
- [ ] Add/Edit **modal** form (amount, date, category) with client validation.
- [ ] Create/update via API + cache invalidation/refresh.
- [ ] Delete with confirm prompt.

**Exit:** add/edit/delete work end to end and the dashboard updates live.

---

## Phase 8 — Filters, Search & CSV Download

**Goal:** finding and exporting expenses.

- [ ] Filter bar: date range, category, amount range, search — bound to list query params.
- [ ] Filters also drive charts/summary for the selected scope.
- [ ] **Export CSV** button hitting the export endpoint with current filters.

**Exit:** filtering/search refine list + charts; CSV downloads the filtered set.

---

## Phase 9 — Polish, Testing & Hardening

**Goal:** production-ready v1.

- [ ] Responsive/visual polish vs. wireframes; accessibility pass (labels, focus, contrast).
- [ ] Frontend component tests (Vitest + Testing Library) for form, list, charts.
- [ ] End-to-end happy-path check (add → see on dashboard → filter → export).
- [ ] Money-precision review across totals/aggregations.
- [ ] Update docs/README; finalize Docker Compose for full local run.

**Exit:** v1 feature-complete, tested, and runnable locally via documented steps.

---

## Milestone Summary

| Phase | Milestone                          | Layer        |
|-------|------------------------------------|--------------|
| 0     | Setup & tooling                    | Infra        |
| 1     | Data model & persistence           | Backend      |
| 2     | CRUD API                           | Backend      |
| 3     | List/filter/pagination + summaries | Backend      |
| 4     | CSV export                         | Backend      |
| 5     | Frontend foundation                | Frontend     |
| 6     | Dashboard & charts                 | Frontend     |
| 7     | List + add/edit/delete             | Frontend     |
| 8     | Filters, search & export UI        | Frontend     |
| 9     | Polish, testing & hardening        | Full-stack   |

## Out of Scope (v1)

Auth/multi-user, recurring expenses, attachments, CSV import, multi-currency,
budgets/limits — per `requirements.md`.
