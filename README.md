# Expense Tracker

A single-user expense tracker: a **React SPA** talking to a **Spring Boot REST API**
backed by **PostgreSQL**. Money is tracked in INR as exact decimals end to end.

This is a monorepo:

- [`backend/`](backend/) — Spring Boot 3.5 REST API (Java 21, Spring Web + Data JPA,
  Bean Validation, Flyway). See [`backend/README.md`](backend/README.md).
- [`frontend/`](frontend/) — Vite + React 19 + TypeScript SPA (Tailwind v4, Recharts,
  TanStack Query, axios, date-fns). See [`frontend/README.md`](frontend/README.md).
- [`docs/`](docs/) — design/spec documents (the source of truth) and wireframe mockups.

The design docs are authoritative. Start with [`docs/requirements.md`](docs/requirements.md),
[`docs/technical-architecture.md`](docs/technical-architecture.md), and
[`docs/api-contracts.md`](docs/api-contracts.md). See [`todo.md`](todo.md) for the phased
build plan.

## Features (v1)

- Add, edit and delete expenses (amount in INR, date, fixed category set).
- Single scrollable dashboard focused on the selected month: "This Month" total,
  a category breakdown donut, and a spending-trend chart — all computed server-side.
- Filter and search the expense list (date range, category, amount range, free-text)
  with the dashboard charts and summary scoped to the same filters.
- Month navigation (prev/next) defaulting to the current month.
- Export the current (filtered) set as a CSV download.
- Loading skeletons, graceful error + retry states, and an empty state.

> Out of scope for v1: auth/multi-user, recurring expenses, attachments, CSV import,
> multi-currency, and budgets/limits.

## How this was built

The entire application — backend, frontend, tests, CI and these docs — was built
**autonomously by Claude Code subagents** driven from a **single prompt**,
[`prompt.txt`](prompt.txt), on top. That prompt walks `todo.md` phase by phase and
dispatches one subagent per task (each in its own context) using the repo's
`/implement-todo` skill, runs the per-phase exit checks, and opens, reviews and
merges a PR for every task. The design documents under [`docs/`](docs/) are the
source of truth the subagents implemented against.

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
on port `5432`, all overridable via environment variables. Copy
[`.env.example`](.env.example) to `.env` to customize them (`.env` is git-ignored and
read by both Docker Compose and the backend's `application.yml`).

### 2. Run the backend

```bash
cd backend
./mvnw spring-boot:run      # serves the API on http://localhost:8080
```

Verify it's up:

```bash
curl http://localhost:8080/api/health   # -> {"status":"OK"}
```

Flyway applies the schema migrations on startup; no manual DB setup is needed beyond
the running Postgres container.

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev                 # Vite dev server on http://localhost:5173
```

Open http://localhost:5173. The dev server proxies `/api` requests to the backend at
`http://localhost:8080` (override with `VITE_API_PROXY_TARGET`), so the SPA uses
same-origin relative URLs and avoids browser CORS in dev.

## Build, test & lint

### Backend (`backend/`)

```bash
./mvnw verify               # unit + integration tests (Testcontainers Postgres) + JaCoCo + Spotless check
./mvnw test                 # faster inner-loop unit tests
./mvnw spotless:apply       # auto-fix Java formatting
./mvnw clean package        # build a runnable jar
```

> Integration tests use Testcontainers, so **Docker must be running** for `verify`.

### Frontend (`frontend/`)

```bash
npm run build               # type-check + production build
npm run lint                # ESLint
npm run format:check        # Prettier (check only)
npm run test                # Vitest unit/component tests (jsdom + Testing Library + MSW)
npm run test:coverage       # Vitest with v8 coverage
npm run test:e2e            # Playwright E2E (builds + serves the SPA against a mocked backend)
```

## Continuous integration

[GitHub Actions](.github/workflows/ci.yml) runs on every push and pull request to
`main`:

- **Backend** — `./mvnw verify` (unit + integration tests with Testcontainers Postgres,
  JaCoCo coverage, and the Spotless format check).
- **Frontend** — `npm ci`, `npm run lint`, `npm run format:check`, `npm run test:coverage`,
  and `npm run build`.
- **E2E** — Playwright (Chromium) against the production preview build driven by a
  deterministic mocked backend.

See [`docs/testing-plan.md`](docs/testing-plan.md) for the test pyramid and CI gates.
