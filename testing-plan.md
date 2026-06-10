# Expense Tracker — Testing Plan

_Last updated: 2026-06-10_

## Strategy

A **full test pyramid**: a broad base of fast unit tests, a solid layer of
integration tests (real Postgres via Testcontainers, API contract tests), and a
focused set of **Playwright** end-to-end tests over the key user flows. Plus
explicit **money/decimal precision** tests since this is an expense app.

```
        ▲   E2E (Playwright) — few, key flows
       ─┼─  Integration (API + DB, FE+API) — moderate
      ──┼── Unit (services, utils, components) — many
```

## Tools

| Layer            | Backend                                   | Frontend                          |
|------------------|-------------------------------------------|-----------------------------------|
| Unit             | JUnit 5, Mockito                          | Vitest + Testing Library          |
| Integration      | Spring Boot Test, `@WebMvcTest`, **Testcontainers** (Postgres) | MSW (mock API) for hook/component integration |
| Contract         | MockMvc against `api-contracts.md`        | Typed client tests                |
| E2E              | —                                         | **Playwright** (real BE + DB)     |
| Coverage         | JaCoCo                                     | Vitest coverage (v8)              |

---

## 1. Backend — Unit Tests

- **Service layer** (`ExpenseService`): create/update/delete logic, defaulting
  (timestamps), and validation paths, with the repository mocked.
- **Aggregation logic**: category breakdown percentages, monthly totals, trend
  grouping (day vs month).
- **CSV generation**: correct columns, escaping, INR 2-decimal formatting.
- **Mappers/DTOs**: entity ⇆ DTO mapping correctness.

## 2. Backend — Integration Tests (Testcontainers)

Run against a **real PostgreSQL** container so SQL, indexes, and Flyway
migrations are exercised.

- **Repository**: persistence, derived/custom filter queries (date range,
  category, amount range), sorting, pagination.
- **Migrations**: Flyway applies cleanly; schema matches entity expectations.
- **Aggregation queries**: `/summary`, `/summary/by-category`, `/summary/trend`
  return correct values across edge cases (empty month, single category, mixed).

## 3. Backend — API / Contract Tests

Validate each endpoint against `api-contracts.md` (MockMvc / full slice):

- **CRUD**: `POST` (201 + body), `GET /{id}` (200/404), `PUT` (200/400/404),
  `DELETE` (204/404).
- **List**: query params, pagination response shape, defaults (current month).
- **Summaries**: response shapes + values; range defaulting.
- **Export**: `Content-Type: text/csv`, `Content-Disposition`, body content.
- **Validation/errors**: amount ≤ 0, missing/invalid fields, bad category →
  uniform error JSON with `fieldErrors`.
- **Categories** reference endpoint returns the fixed enum set.

## 4. Frontend — Unit / Component Tests (Vitest + Testing Library)

- **Add/Edit modal**: renders fields, required-field validation, amount > 0,
  submit calls the API client, edit pre-fills values.
- **Expense table**: rows render, edit/delete actions fire, empty state shows.
- **Charts**: donut/trend receive and render summary data; empty/zero state.
- **Filters**: changing date range/category/amount updates query params.
- **Hooks**: TanStack Query hooks with **MSW**-mocked API — loading, success,
  error, and cache-invalidation-on-mutation behavior.
- **Formatting utils**: INR display, date formatting/grouping.

## 5. End-to-End — Playwright

Run against a real backend + Postgres (Docker Compose), seeded with known data.
Key flows:

1. **Add expense** → appears in list; total, donut, and trend update.
2. **Edit expense** → modal pre-fills, save updates row + summaries.
3. **Delete expense** → confirm prompt → row removed, totals update.
4. **Filter & search** → list and charts reflect date range / category / amount.
5. **Month navigation** → dashboard scopes to selected month; empty state for an
   empty month.
6. **Export CSV** → download triggered; file contains the filtered rows.
7. **Loading/error** → skeletons during fetch; graceful error + retry on failure.

## 6. Money / Decimal Precision (explicit)

Critical for an expense tracker — guard against floating-point drift end to end.

- **Storage**: `NUMERIC(12,2)` / `BigDecimal`; round-trip preserves exact values
  (e.g. `0.10 + 0.20 = 0.30`, large sums, many small amounts).
- **Aggregation**: category totals and grand totals sum exactly; percentages
  computed from exact totals and sum to ~100% (documented rounding rule).
- **Boundaries**: amount `0.01` accepted, `0` / negative rejected; high-value
  amounts within `NUMERIC(12,2)` range.
- **CSV**: amounts always rendered with two decimals; no scientific notation.
- **Frontend display**: INR formatting matches stored value (no rounding errors
  in the UI layer).

---

## Test Data & Environments

- **Unit**: in-memory/mocked, no external deps.
- **Integration/E2E**: ephemeral Postgres via Testcontainers (BE tests) and
  Docker Compose (E2E), seeded with a deterministic fixture set spanning
  multiple months and categories.
- **Isolation**: each test resets/owns its data; no shared mutable state.

## CI Gates

- Backend: `mvnw verify` (unit + integration + JaCoCo) must pass.
- Frontend: `vitest run --coverage` + lint must pass.
- E2E: Playwright suite runs in CI against a composed stack (can be a separate,
  required job).
- **Merge blocked** on failing tests; coverage thresholds set per layer
  (tuned during build).

## Definition of Done (testing)

- All in-scope features (`requirements.md`) covered by at least unit +
  integration tests; key flows covered by E2E.
- Money-precision suite green.
- CI green on all gates.

## Out of Scope (v1)

Load/stress testing, security/pen testing, and cross-browser matrix beyond the
Playwright default browser — deferred (single-user, local app).
