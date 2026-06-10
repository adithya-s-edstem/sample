# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

This is a **planning/specification workspace** for an **Expense Tracker** application. There is **no application code yet** — the repo currently contains only design documents (Markdown) and static HTML/CSS wireframe mockups. There are no build, lint, or test commands because nothing has been scaffolded. The implementation is expected to be built out per `implementation-plan.md`.

The docs are the source of truth and were authored in a deliberate order (see `plan.md`). When implementing, treat them as the spec; if you change a decision, update the relevant doc so the docs stay authoritative.

## Document map

Read these to understand the intended system before writing any code:

- `requirements.md` — product scope, data model, in/out-of-scope features. **Note:** it originally said "local-only / no backend"; this was superseded — see the architecture doc.
- `technical-architecture.md` — the binding stack decision and DB schema. **Supersedes** the storage decision in `requirements.md`: data is persisted server-side in PostgreSQL via a Spring Boot API.
- `solution.md` — UX/layout: single scrollable page, current-month focus, add/edit modal.
- `api-contracts.md` — REST endpoints, DTOs, validation rules, error shape. Implementations must match this exactly.
- `wireframes.md` + `wireframes/` — rendered HTML/CSS mockups (`dashboard.html`, `modal.html`, `empty.html`, `loading.html`, shared `styles.css`). These are **design references only**, not the production frontend (which will be React/Tailwind/Recharts).
- `implementation-plan.md` — phased, backend-first build plan (Phase 0 setup → Phase 9 hardening) with the intended monorepo layout (`/backend`, `/frontend`, `/docs`).
- `testing-plan.md` — test pyramid strategy and CI gates.

## Intended architecture (per the docs)

React SPA (Vite + TypeScript + Tailwind + Recharts + TanStack Query) → Spring Boot 3.x REST API (Java 21, Spring Web + Data JPA, Bean Validation, Flyway) → PostgreSQL. Single source of truth is the DB; the frontend only talks to the API. Single-user, **no auth in v1**. Planned monorepo: `/backend`, `/frontend`, `/docs`, with Docker Compose for local Postgres.

## Conventions that must hold across the codebase

These are cross-cutting rules from the specs — get them right wherever money or the API is touched:

- **Money is exact decimal end-to-end.** Use `BigDecimal` / `NUMERIC(12,2)` server-side; never floats. Amounts are INR, two decimals, must be `> 0`. There is an explicit money-precision test suite requirement.
- **API base path is `/api`**; JSON everywhere except the CSV export endpoint. IDs are UUID strings. Dates are `YYYY-MM-DD`; timestamps are ISO-8601 UTC.
- **Categories are a fixed enum** (`FOOD, TRANSPORT, RENT, UTILITIES, GROCERIES, ENTERTAINMENT, HEALTH, SHOPPING, OTHER`) — no custom categories in v1.
- **Errors use one uniform shape** (`timestamp, status, error, message, path, fieldErrors[]`) via a global exception handler.
- **List/summary/export endpoints default to the current month** when `from`/`to` are omitted. Summary endpoints (`/summary`, `/summary/by-category`, `/summary/trend`) return ready-to-chart JSON computed server-side.
- **Backend-first build order**: build and test the API against `api-contracts.md`, then build the frontend against the live API.

## Build/run/test

Not yet applicable — no project scaffolding exists. Once Phase 0 of `implementation-plan.md` is done, the intended commands are:
- Frontend: `npm run dev` (Vite, proxies `/api` to backend), `npm run build`.
- Backend: `./mvnw spring-boot:run`, `./mvnw clean package`, `./mvnw verify` (unit + integration + JaCoCo).
- DB: PostgreSQL via Docker Compose.

Update this section with the real commands once they exist.
