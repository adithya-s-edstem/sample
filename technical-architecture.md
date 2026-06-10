# Expense Tracker — Technical Architecture

_Last updated: 2026-06-10_

## 1. Overview

A client–server web application with a **React single-page app (SPA) frontend**
and a **Java (Spring Boot) REST API backend** backed by a **PostgreSQL**
database. The database is the single source of truth; the frontend reads and
writes exclusively through the REST API.

> Note: this supersedes the earlier "local-only / no backend" decision in
> `requirements.md`. Data is now persisted server-side in PostgreSQL.

## 2. High-Level Architecture

```
┌──────────────────────┐      HTTP/JSON       ┌──────────────────────┐
│   React SPA (Vite)   │  ───────────────▶    │  Spring Boot REST API │
│  Tailwind, Recharts  │  ◀───────────────    │   (Java, Spring Web)  │
└──────────────────────┘                      └───────────┬──────────┘
                                                           │ JPA / Hibernate
                                                           ▼
                                                ┌──────────────────────┐
                                                │      PostgreSQL       │
                                                └──────────────────────┘
```

## 3. Frontend Stack

| Layer        | Choice                  | Why                                                            |
|--------------|-------------------------|----------------------------------------------------------------|
| Language     | **TypeScript**          | Type safety for the data model and app logic.                  |
| UI framework | **React 18**            | Mature ecosystem; strong charts/forms support.                 |
| Build tool   | **Vite**                | Fast dev server + optimized static production build.           |
| Styling      | **Tailwind CSS**        | Utility-first; clean, minimal, consistent UI.                  |
| Charts       | **Recharts**            | Declarative React charts (pie/donut + bar/line).               |
| HTTP client  | **fetch / axios**       | Calls the backend REST API.                                    |
| Data fetching| **TanStack Query**      | Caching, loading/error states, and refetch for API calls.      |

### Frontend libraries
- `react`, `react-dom`, `vite`, `@vitejs/plugin-react`, `typescript`
- `tailwindcss`, `postcss`, `autoprefixer`
- `recharts`
- `@tanstack/react-query` — server-state management around the REST API
- `axios` (or native `fetch`) — HTTP calls
- `date-fns` — date math for month grouping/ranges/formatting
- Dev/quality: `eslint`, `@typescript-eslint`, `prettier`, `vitest`,
  `@testing-library/react`

## 4. Backend Stack

| Layer          | Choice                       | Why                                                        |
|----------------|------------------------------|------------------------------------------------------------|
| Language       | **Java 21**                  | LTS, modern language features.                             |
| Framework      | **Spring Boot 3.x**          | Batteries-included REST APIs, huge ecosystem.              |
| Web            | **Spring Web (MVC)**         | REST controllers, JSON serialization (Jackson).            |
| Persistence    | **Spring Data JPA / Hibernate** | Repository abstraction; least boilerplate for CRUD.     |
| Validation     | **Jakarta Bean Validation**  | Declarative request validation (`@Valid`, constraints).    |
| Build          | **Maven** (or Gradle)        | Standard Java build + dependency management.               |
| Migrations     | **Flyway**                   | Versioned, repeatable DB schema migrations.                |
| Testing        | **JUnit 5, Spring Boot Test, Testcontainers** | Unit + integration tests against real Postgres. |

### Backend dependencies (Maven starters)
- `spring-boot-starter-web` — REST endpoints + JSON.
- `spring-boot-starter-data-jpa` — JPA/Hibernate repositories.
- `spring-boot-starter-validation` — bean validation.
- `postgresql` — JDBC driver.
- `flyway-core` — schema migrations.
- `spring-boot-starter-test`, `org.testcontainers:postgresql` — testing.
- (Optional) `springdoc-openapi-starter-webmvc-ui` — auto-generated OpenAPI/Swagger docs.

## 5. Backend Architecture (layers)

1. **Controller layer** (`controller/`) — REST endpoints, request/response DTOs,
   validation, HTTP status mapping.
2. **Service layer** (`service/`) — business logic (totals, monthly summaries,
   category aggregation, CSV export generation).
3. **Repository layer** (`repository/`) — Spring Data JPA repositories for the
   `Expense` entity, including derived/custom queries for filtering and
   aggregation.
4. **Domain / entity layer** (`domain/`) — JPA entities and the `Category` enum.
5. **DTO + mapping** (`dto/`) — request/response objects kept separate from
   entities; mapped explicitly (or via MapStruct).

## 6. Data Storage Design (PostgreSQL)

### Table: `expenses`
| Column       | Type            | Notes                                            |
|--------------|-----------------|--------------------------------------------------|
| `id`         | `UUID` (PK)     | Generated server-side.                           |
| `amount`     | `NUMERIC(12,2)` | INR; exact decimal — avoids float rounding.      |
| `date`       | `DATE`          | Expense date (the day money was spent).          |
| `category`   | `VARCHAR`/enum  | One of the predefined categories.                |
| `created_at` | `TIMESTAMPTZ`   | Audit/order.                                     |
| `updated_at` | `TIMESTAMPTZ`   | Audit.                                           |

### Indexes
- Index on `date` — date-range filtering and monthly grouping.
- Index on `category` — category filtering and breakdowns.

### JPA entity (sketch)
```java
@Entity
@Table(name = "expenses")
public class Expense {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;          // INR, exact decimal

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
}
```

```java
public enum Category {
    FOOD, TRANSPORT, RENT, UTILITIES, GROCERIES,
    ENTERTAINMENT, HEALTH, SHOPPING, OTHER
}
```

### Notes
- **Currency**: single currency (INR ₹); not stored per row — applied at the
  display layer. `BigDecimal` / `NUMERIC` used end-to-end for exact money math.
- **Migrations**: schema created and evolved via Flyway scripts
  (`src/main/resources/db/migration/V1__init.sql`, …).

## 7. API Boundary

- REST + JSON over HTTP. Endpoints expose CRUD on expenses plus aggregation
  (monthly summary, category breakdown) and CSV export.
- **CORS** configured to allow the Vite dev origin in development.
- Full endpoint definitions live in `api-contracts.md` (next section).

## 8. Build & Run

### Frontend
- **Dev**: `npm run dev` (Vite dev server, proxies `/api` to the backend).
- **Build**: `npm run build` → static `dist/`.

### Backend
- **Dev**: `./mvnw spring-boot:run`.
- **Build**: `./mvnw clean package` → runnable jar.
- **DB**: PostgreSQL (local install or Docker); connection configured via
  `application.yml` / environment variables.

### Local orchestration
- **Docker Compose** recommended for local dev: one service for PostgreSQL,
  optionally one for the packaged backend. Frontend runs via Vite or is served
  as static assets.

## 9. Privacy & Deployment

- Single-user, personal use — no authentication in v1 (data is not exposed
  beyond the user's own environment).
- Designed to run locally (e.g. on the user's machine via Docker Compose); can
  be deployed to a private server later.
- No third-party analytics or external data sharing.

## 10. Open / Deferred Decisions

- Authentication/authorization (intentionally out of scope for v1 single-user).
- Maven vs Gradle (defaulting to Maven).
- Whether to add an offline cache layer in the SPA later (not in v1).
- Deployment target (local Docker for now; cloud later if needed).
