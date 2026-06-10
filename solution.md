# Expense Tracker — Solution

_Last updated: 2026-06-10_

## 1. Solution Summary

A single-page expense tracker. The user lands on one scrollable view that shows
the **current month** at a glance: this month's total, a category breakdown
(donut), a spending trend, and the list of expenses. Adding or editing an
expense happens in a focused **modal dialog** without leaving the page.
Everything is backed by the Spring Boot + PostgreSQL API.

## 2. Layout — Single Page, All-in-One

One scrollable page, top to bottom:

```
┌─────────────────────────────────────────────────────────────┐
│  Header:  "Expense Tracker"      [ Month: June 2026 ▼ ]  [+ Add] │
├─────────────────────────────────────────────────────────────┤
│  SUMMARY ROW                                                  │
│  ┌───────────────┐  ┌───────────────────────────────────┐    │
│  │ This month     │  │  Category breakdown (donut)        │    │
│  │   ₹ 24,500     │  │   ● Food  ● Rent  ● Transport ...  │    │
│  └───────────────┘  └───────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│  TREND                                                        │
│  ┌───────────────────────────────────────────────────────┐   │
│  │  Spending over time (bar/line)                          │   │
│  └───────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│  FILTERS:  [date range]  [category ▼]  [min–max amount]  [⌕]  │
├─────────────────────────────────────────────────────────────┤
│  EXPENSE LIST                                  [ Export CSV ] │
│  Date        Category      Amount        Actions              │
│  10 Jun      Groceries     ₹ 1,200       ✎  🗑               │
│  09 Jun      Transport     ₹   300       ✎  🗑               │
│  ...                                                          │
└─────────────────────────────────────────────────────────────┘
```

- **Header**: app title, a **month selector** (defaults to current month), and
  a primary **+ Add** button.
- **Summary row**: this month's total (large) + category donut.
- **Trend**: spending-over-time chart for the selected period.
- **Filters**: date range, category, amount range, and search.
- **Expense list**: rows with edit/delete; CSV export action.

The default state focuses on the **current month**; the month selector and
filters let the user look at other periods.

## 3. Add / Edit — Modal Dialog

- Clicking **+ Add** (or the ✎ edit icon on a row) opens a centered **modal**.
- The modal contains the form fields:
  - **Amount** (required, numeric, INR)
  - **Date** (required, date picker, defaults to today)
  - **Category** (required, dropdown of predefined categories)
- Actions: **Save** and **Cancel**. Validation errors shown inline.
- On save, the modal closes, the API is called, and the dashboard + list
  refresh (via TanStack Query cache invalidation).
- Edit mode pre-fills the form with the existing expense; Save issues an update.

## 4. Delete

- The 🗑 icon on a row triggers a small **confirm** prompt before deleting.
- On confirm, the API delete is called and the view refreshes.

## 5. Dashboard Emphasis — Total + Category Donut

The top of the page leads with **where the money went this month**:

1. **This month's total** — the single most important number, shown large.
2. **Category breakdown (donut)** — proportional split across categories, with a
   legend/labels and amounts on hover. Directly answers "where does my money go?"
3. **Recent / full expense list** below for detail.

The trend chart sits beneath the summary as secondary context.

## 6. Core User Flows

### Add an expense
1. Click **+ Add** → modal opens (date defaults to today).
2. Enter amount, pick date, choose category.
3. **Save** → expense created via API → total, donut, trend, and list update.

### Review spending (default)
1. Open app → current month shown.
2. Read total + donut to see the breakdown.
3. Scroll the list / scan the trend.

### Edit / delete
1. Click ✎ on a row → modal pre-filled → adjust → Save.
2. Click 🗑 → confirm → removed.

### Filter / search
1. Set date range, category, and/or amount range; type in search.
2. List and charts update to reflect the filtered set.

### Export
1. Click **Export CSV** → downloads the current (filtered) expenses as a CSV.

## 7. Feature → Solution Mapping

| Requirement (v1)            | Where it lives in the solution                          |
|-----------------------------|---------------------------------------------------------|
| Add/edit/delete expenses    | Modal form + row actions + confirm on delete            |
| Charts & reports            | Category donut + spending trend; monthly total          |
| Search & filter             | Filter bar (date range, category, amount) + search      |
| Export to CSV               | Export button (respects active filters)                 |
| Current-month dashboard     | Default view scoped to current month via month selector |

## 8. Visual Style

- Clean and minimal: generous whitespace, neutral palette, readable charts.
- Tailwind utility classes for consistent spacing/typography.
- Modal is lightweight and centered; no heavy chrome.

## 9. States & Edge Cases

- **Empty state**: no expenses this month → friendly prompt with a **+ Add**
  call-to-action; charts show an empty/zero state.
- **Loading**: skeletons/spinners while the API responds (TanStack Query).
- **Error**: inline error message with retry if an API call fails.
- **Validation**: required fields enforced client-side and server-side;
  amount must be positive.

## 10. Out of Scope (per requirements)

Recurring expenses, attachments, CSV import, multi-currency, budgets/limits,
multi-user/accounts.
