# frontend — Expense Tracker SPA

React SPA (Vite + TypeScript + Tailwind + Recharts + TanStack Query) for the
Expense Tracker. Talks only to the backend API under `/api`.

> **Status: placeholder.** This directory is the monorepo slot for the frontend
> created in **P0-1**. The Vite project itself is scaffolded in **P0-5**
> (React + TS, plus Tailwind, Recharts, TanStack Query, axios, date-fns), with
> the dev proxy `/api` → backend wired in **P0-6**.

See [`../docs/implementation-plan.md`](../docs/implementation-plan.md) for the
phased build plan and [`../docs/solution.md`](../docs/solution.md) for the UX/layout.

## Intended commands (once scaffolded)

- `npm run dev` — Vite dev server (proxies `/api` to the backend)
- `npm run build` — production build
- `npm run test` — Vitest
