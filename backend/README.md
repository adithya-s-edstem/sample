# backend — Expense Tracker API

Spring Boot 3.5.x REST API (Java 21) backing the Expense Tracker. All endpoints
match [`../docs/api-contracts.md`](../docs/api-contracts.md) exactly.

## Prerequisites

- JDK 21
- A running PostgreSQL (use the repo-root `docker compose up -d`) for
  `spring-boot:run`. Tests spin up their own Postgres via Testcontainers
  (Docker required).

## Commands

- `./mvnw spring-boot:run` — run the API locally (needs the DB up)
- `./mvnw clean package` — build a runnable jar
- `./mvnw verify` — unit + integration tests (Testcontainers Postgres) + JaCoCo + Spotless check
- `./mvnw test` — faster inner-loop unit tests
- `./mvnw spotless:apply` — auto-fix Java formatting (Palantir, LF line endings)

## API surface

Base path `/api`; JSON everywhere except the CSV export. IDs are UUID strings,
dates are `YYYY-MM-DD`, timestamps are ISO-8601 UTC, money is `BigDecimal` /
`NUMERIC(12,2)` (INR, `> 0`). Errors use one uniform shape
(`timestamp, status, error, message, path, fieldErrors[]`).

- `GET /api/health` — liveness probe.
- `POST /api/expenses`, `GET /api/expenses/{id}`, `PUT /api/expenses/{id}`,
  `DELETE /api/expenses/{id}` — CRUD.
- `GET /api/expenses` — filtered/sorted/paginated list (defaults to the current month,
  `date,desc`, size cap 200).
- `GET /api/expenses/export` — streamed CSV download (same filters, no pagination).
- `GET /api/categories` — the fixed `Category` enum set.
- `GET /api/summary`, `GET /api/summary/by-category`, `GET /api/summary/trend` —
  server-side aggregations returning ready-to-chart JSON.

List, summary and export endpoints default to the current month when `from`/`to`
are omitted.

## Layout

`src/main/java/com/expensetracker/` (conventional layered structure):

- `domain/` — `Category` enum + `Expense` JPA entity.
- `repository/` — `ExpenseRepository` (CRUD, filter specs, aggregation queries,
  cursor-backed `streamAll` for export) + `ExpenseSpecifications`; `repository/projection/`
  holds the aggregation projections.
- `service/` — `ExpenseService` business logic + `ExpenseNotFoundException`.
- `web/` — `@RestController`s (`ExpenseController`, `SummaryController`,
  `CategoryController`, `HealthController`) with subpackages `web/dto/`, `web/mapper/`,
  `web/error/` (`ApiError` + `GlobalExceptionHandler`), `web/csv/` (`ExpenseCsvWriter`).
- `config/` — `WebCorsConfig` (CORS for the Vite dev origin).

Schema is **Flyway-owned**: migrations live in `src/main/resources/db/migration/`
(`V1__init.sql`). Hibernate runs `ddl-auto=validate` and never mutates the schema —
change the DB only by adding a new `V__` migration.

See [`../docs/implementation-plan.md`](../docs/implementation-plan.md) and
[`../docs/technical-architecture.md`](../docs/technical-architecture.md).
