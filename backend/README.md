# backend — Expense Tracker API

Spring Boot 3.5.x REST API (Java 21) backing the Expense Tracker.

Scaffolded in **P0-3** via Spring Initializr with: Spring Web, Spring Data JPA,
Validation, PostgreSQL driver, Flyway, and Testcontainers. Datasource / JPA /
Flyway configuration (`application.yml`) is wired in **P0-4**; the data model and
migrations land in Phase 1.

## Prerequisites

- JDK 21
- A running PostgreSQL (use the repo-root `docker compose up -d`) for
  `spring-boot:run`. Tests spin up their own Postgres via Testcontainers
  (Docker required).

## Commands

- `./mvnw spring-boot:run` — run the API locally (needs the DB up + `application.yml` from P0-4)
- `./mvnw clean package` — build a runnable jar
- `./mvnw verify` — unit + integration tests (Testcontainers Postgres) + JaCoCo
- `./mvnw test` — faster inner-loop unit tests

## Layout

- `src/main/java/com/expensetracker` — application code (package `com.expensetracker`)
- `src/main/resources/db/migration` — Flyway SQL migrations (`V1__init.sql` in P1-3)
- `src/test/java/com/expensetracker` — tests, incl. `TestcontainersConfiguration`

See [`../docs/implementation-plan.md`](../docs/implementation-plan.md) and
[`../docs/technical-architecture.md`](../docs/technical-architecture.md).
