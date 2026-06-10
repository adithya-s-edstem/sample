# Expense Tracker — Requirements

_Last updated: 2026-06-10_

## 1. Overview

A personal expense tracker whose primary goal is to **track where money goes** —
recording expenses and seeing spending breakdowns. It is a single-user,
local-only web application with a clean, minimal interface.

## 2. Platform & Architecture

| Aspect    | Decision                                                        |
|-----------|-----------------------------------------------------------------|
| Platform  | Web app (runs in the browser)                                   |
| Users     | Single user — personal use, **no login/accounts required**      |
| Storage   | **Local only** — data stays on the device, no backend/sync needed (e.g. browser local storage / local file) |
| Privacy   | No internet connection required; no data leaves the device      |

## 3. Primary Goal

- Record and categorize expenses.
- See clear breakdowns of spending (where the money goes).
- Budgeting/limits are **out of scope** for now.

## 4. Expense Data Model

Each expense record has:

- **Amount** (required)
- **Date** (required)
- **Category** (required)

> Description/notes, payment method, and merchant/payee were **not** requested
> and are out of scope for v1.

### Currency
- **Single currency** only.
- Default currency: **INR (₹)**.

### Categories
- **Predefined categories** (a fixed set). No custom categories in v1.
- Suggested default set (to be confirmed during build):
  Food, Transport, Rent, Utilities, Groceries, Entertainment, Health, Shopping, Other.

## 5. Features (v1)

In scope:

- **Add / edit / delete expenses** with the fields above.
- **Charts & reports** — visual breakdown by category and spending over time;
  monthly summaries.
- **Search & filter** — find expenses by date range, category, and amount.
- **Export to CSV** — download expenses as a spreadsheet-friendly file.

Out of scope for v1:

- Recurring expenses (auto-tracking subscriptions/bills)
- Receipt/notes attachments
- CSV import
- Multi-currency support
- Budgets / spending limits
- Multi-user / shared accounts

## 6. Dashboard / Main View

- Primary view focuses on the **current month**.
- Shows this month's total spending and a category breakdown.
- Charts and the expense list reflect the current month by default; filters
  allow looking at other periods.

## 7. Visual Style

- **Clean & minimal**: simple layout, generous whitespace, neutral color palette.
- Charts kept readable and uncluttered.

## 8. Open Questions / To Confirm Later

- Exact list of predefined categories.
- Where local data is persisted (browser `localStorage`, IndexedDB, or a local file).
- Chart types to include (pie/donut for category split, bar/line for trend).
- Whether the dashboard period selector should support arbitrary custom ranges later.
