# frontend — Expense Tracker SPA

React SPA (Vite + React 19 + TypeScript) for the Expense Tracker. Talks only to the
backend API under `/api`.

| Concern      | Library                                  |
| ------------ | ---------------------------------------- |
| Styling      | Tailwind CSS v4 (`@tailwindcss/vite`)    |
| Charts       | Recharts                                 |
| Server state | TanStack Query (`@tanstack/react-query`) |
| HTTP         | axios                                    |
| Dates        | date-fns                                 |

The UI is a single scrollable dashboard scoped to a selected month: a "This Month"
total card, a category donut and a spending-trend chart (all from the server-side
summary endpoints), a filter bar (date range / category / amount range / search), the
expense list with add/edit modal and delete-with-confirm, and a CSV export button.

## Prerequisites

- Node.js 22+ / npm

## Commands

- `npm install` — install dependencies
- `npm run dev` — Vite dev server (proxies `/api` to the backend at `http://localhost:8080`,
  override with `VITE_API_PROXY_TARGET`)
- `npm run build` — type-check + production build to `dist/`
- `npm run preview` — preview the production build
- `npm run lint` — ESLint
- `npm run format:check` — Prettier (check only)
- `npm run test` — Vitest unit/component tests (jsdom + Testing Library + MSW)
- `npm run test:coverage` — Vitest with v8 coverage
- `npm run test:e2e` — Playwright E2E against the production preview build

## Layout

- `src/main.tsx` — entry; wires `QueryClientProvider`, `MonthProvider`, `FilterProvider`
- `src/App.tsx` — the dashboard composition + modal/delete state
- `src/api/` — typed API layer (`types.ts`, axios `client.ts`, endpoint wrappers
  `expenses.ts`, `queryKeys.ts`)
- `src/hooks/` — TanStack Query hooks (`useExpenses`, `useSummary`, `useCategories`,
  the create/update/delete mutations)
- `src/context/` — `MonthContext` (selected month) and `FilterContext` (filter-bar state)
- `src/components/layout/` — `Header`, `Card`, `Skeleton`, `ErrorState`
- `src/components/dashboard/` — summary row, charts (`CategoryDonut`, `TrendChart`),
  the expense list/rows, the add/edit modal (`ExpenseModal` + `ExpenseFormModal`),
  delete dialog, filter bar, empty state
- `src/lib/` — pure helpers (`money`, `category`, `month`, `filters`)
- `src/test/` — Vitest setup + MSW server/handlers + render helpers
- `e2e/` — Playwright specs and the mocked-backend fixtures

See [`../docs/solution.md`](../docs/solution.md) for the UX/layout and
[`../docs/wireframes.md`](../docs/wireframes.md) for the mockups.
