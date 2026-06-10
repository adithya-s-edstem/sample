# Expense Tracker

A single-user expense tracker: a **React SPA** talking to a **Spring Boot REST API**
backed by **PostgreSQL**. Money is tracked in INR as exact decimals end to end.

This is a monorepo:

- [`backend/`](backend/) — Spring Boot 3.5 REST API (Java 21, Spring Web + Data JPA,
  Bean Validation, Flyway). See [`backend/README.md`](backend/README.md).
- [`frontend/`](frontend/) — Vite + React + TypeScript SPA (Tailwind, Recharts,
  TanStack Query, axios, date-fns).
- [`docs/`](docs/) — design/spec documents (the source of truth) and wireframe mockups.

The design docs are authoritative. Start with [`docs/requirements.md`](docs/requirements.md),
[`docs/technical-architecture.md`](docs/technical-architecture.md), and
[`docs/api-contracts.md`](docs/api-contracts.md). See [`todo.md`](todo.md) for the phased
build plan.

## Prerequisites

- **JDK 21** (backend)
- **Node.js 22+** and npm (frontend)
- **Docker** — for local Postgres via Docker Compose, and for the backend's
  Testcontainers-based integration tests

## Run it locally

The app runs as three processes during development: Postgres in Docker, the backend
on the host, and the Vite dev server on the host (which proxies `/api` to the backend).

### 1. Start Postgres

From the repo root:

```bash
docker compose up -d        # start Postgres in the background
docker compose down         # stop (data survives in the named volume)
docker compose down -v      # stop and DROP the data volume
```

Connection settings default to DB `expense-tracker`, user/password `expense`/`expense`
on port `5432`, all overridable via environment variables (see
[`docker-compose.yml`](docker-compose.yml)).

### 2. Run the backend

```bash
cd backend
./mvnw spring-boot:run      # serves the API on http://localhost:8080
```

Verify it's up:

```bash
curl http://localhost:8080/api/health   # -> {"status":"OK"}
```

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev                 # Vite dev server on http://localhost:5173
```

The dev server proxies `/api` requests to the backend at `http://localhost:8080`
(override with `VITE_API_PROXY_TARGET`).

## Build, test & lint

### Backend (`backend/`)

```bash
./mvnw verify               # unit + integration tests (Testcontainers Postgres) + Spotless check
./mvnw test                 # faster inner-loop unit tests
./mvnw spotless:apply       # auto-fix Java formatting
./mvnw clean package        # build a runnable jar
```

### Frontend (`frontend/`)

```bash
npm run build               # type-check + production build
npm run lint                # ESLint
npm run format              # Prettier (write)
npm run format:check        # Prettier (check only)
```

## Continuous integration

[GitHub Actions](.github/workflows/ci.yml) runs on every push and pull request to
`main`, with one job per app:

- **Backend** — `./mvnw verify` (tests + Spotless format check).
- **Frontend** — `npm ci`, `npm run lint`, `npm run format:check`, `npm run build`.

Frontend unit tests (Vitest) and the Playwright E2E + coverage gates are added in
later phases as the corresponding features land (see [`docs/testing-plan.md`](docs/testing-plan.md)).
