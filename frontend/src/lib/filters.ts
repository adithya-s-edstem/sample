/*
 * Maps the UI-shaped filter state (context/filterContext) plus the selected
 * month's range onto the typed `ExpenseQuery` params for `GET /api/expenses`
 * (and, later, the CSV export and summaries). Centralized here so the list,
 * charts (P8-2), and export (P8-3) all derive identical query params.
 *
 * Rules:
 * - The date range comes from the selected month by default; an explicit
 *   `from`/`to` filter overrides the corresponding bound when set.
 * - Empty strings mean "no filter" and are dropped so the field is omitted from
 *   the querystring (the backend then applies its own defaults where relevant).
 * - Amounts are exact decimals on the wire (NUMERIC(12,2)); we parse the raw
 *   input to a finite number and omit anything non-numeric.
 */
import type { ExpenseQuery, SummaryQuery } from '../api/types'
import type { FilterState } from '../context/filterContext'
import type { MonthRange } from './month'

/** Parses a raw amount input to a finite number, or `undefined` if blank/invalid. */
function parseAmount(raw: string): number | undefined {
  const trimmed = raw.trim()
  if (trimmed === '') return undefined
  const n = Number(trimmed)
  return Number.isFinite(n) ? n : undefined
}

/** Trims a string and returns `undefined` when empty so the param is omitted. */
function trimOrUndefined(raw: string): string | undefined {
  const trimmed = raw.trim()
  return trimmed === '' ? undefined : trimmed
}

/**
 * Builds the `ExpenseQuery` slice from the filters and the month range. The
 * returned object only carries the filtering params (date range, category,
 * amount range, search); pagination/sort are layered on by the caller.
 */
export function buildExpenseQuery(filters: FilterState, range: MonthRange): ExpenseQuery {
  const query: ExpenseQuery = {
    from: trimOrUndefined(filters.from) ?? range.from,
    to: trimOrUndefined(filters.to) ?? range.to,
  }

  if (filters.category !== '') query.category = filters.category
  const minAmount = parseAmount(filters.minAmount)
  if (minAmount !== undefined) query.minAmount = minAmount
  const maxAmount = parseAmount(filters.maxAmount)
  if (maxAmount !== undefined) query.maxAmount = maxAmount
  const q = trimOrUndefined(filters.q)
  if (q !== undefined) query.q = q

  return query
}

/**
 * Builds the date-range scope (`SummaryQuery`) for the summary/chart endpoints
 * (`/summary`, `/summary/by-category`, `/summary/trend`) from the same filter
 * state and month range. Per docs/api-contracts.md §3, those endpoints only
 * accept the `from`/`to` range (the category/amount/search filters are list-only),
 * so the charts follow the selected date scope: the month range by default, or
 * the explicit `from`/`to` filters when set (P8-2). Deriving this from the shared
 * filter state alongside `buildExpenseQuery` keeps the list and charts in lockstep.
 */
export function buildSummaryQuery(filters: FilterState, range: MonthRange): SummaryQuery {
  return {
    from: trimOrUndefined(filters.from) ?? range.from,
    to: trimOrUndefined(filters.to) ?? range.to,
  }
}
