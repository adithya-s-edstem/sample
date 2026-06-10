# frontend — Expense Tracker SPA

React SPA (Vite + TypeScript) for the Expense Tracker. Talks only to the backend
API under `/api`.

Scaffolded in **P0-5** (Vite `react-ts`) with the production libraries:

| Concern      | Library                                  |
| ------------ | ---------------------------------------- |
| Styling      | Tailwind CSS v4 (`@tailwindcss/vite`)    |
| Charts       | Recharts                                 |
| Server state | TanStack Query (`@tanstack/react-query`) |
| HTTP         | axios                                    |
| Dates        | date-fns                                 |

The dev proxy (`/api` → backend) is added in **P0-6**; the app shell/theme and
data hooks are built from **Phase 5**.

## Prerequisites

- Node 20+ / npm 10+

## Commands

- `npm install` — install dependencies
- `npm run dev` — Vite dev server (will proxy `/api` to the backend after P0-6)
- `npm run build` — type-check + production build to `dist/`
- `npm run preview` — preview the production build
- `npm run lint` — ESLint

## Layout

- `src/main.tsx` — entry; wires the TanStack Query `QueryClientProvider`
- `src/App.tsx` — placeholder shell (replaced in P5-1)
- `src/index.css` — imports Tailwind (`@import 'tailwindcss';`)
- `vite.config.ts` — React + Tailwind plugins

See [`../docs/solution.md`](../docs/solution.md) for the UX/layout and
[`../docs/wireframes.md`](../docs/wireframes.md) for the mockups.
