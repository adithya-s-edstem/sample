# backend — Expense Tracker API

Spring Boot 3.x REST API (Java 21) backing the Expense Tracker.

> **Status: placeholder.** This directory is the monorepo slot for the backend
> created in **P0-1**. The Spring Boot project itself is scaffolded in **P0-3**
> (Spring Initializr: Web, Data JPA, Validation, PostgreSQL driver, Flyway,
> Testcontainers), with `application.yml` wired in **P0-4**.

See [`../docs/implementation-plan.md`](../docs/implementation-plan.md) for the
phased build plan and [`../docs/technical-architecture.md`](../docs/technical-architecture.md)
for the binding stack decision and DB schema.

## Intended commands (once scaffolded)

- `./mvnw spring-boot:run` — run the API locally
- `./mvnw clean package` — build
- `./mvnw verify` — unit + integration + JaCoCo
