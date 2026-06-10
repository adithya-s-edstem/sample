/*
 * Wire types for the Expense Tracker API, mirroring docs/api-contracts.md and the
 * backend DTOs (web/dto/*) exactly. Money values cross the wire as JSON numbers
 * with two decimals (server-side BigDecimal / NUMERIC(12,2)); we keep them as
 * `number` here for charting/display and format them at the UI edge. IDs are UUID
 * strings, dates are `YYYY-MM-DD`, timestamps are ISO-8601 UTC instants.
 */

/** Fixed category enum — must match the backend `Category` (no custom categories in v1). */
export const CATEGORIES = [
  'FOOD',
  'TRANSPORT',
  'RENT',
  'UTILITIES',
  'GROCERIES',
  'ENTERTAINMENT',
  'HEALTH',
  'SHOPPING',
  'OTHER',
] as const

export type Category = (typeof CATEGORIES)[number]

/** A persisted expense (`Expense` response DTO). */
export interface Expense {
  id: string
  amount: number
  /** ISO calendar date `YYYY-MM-DD`. */
  date: string
  category: Category
  /** ISO-8601 UTC instant. */
  createdAt: string
  /** ISO-8601 UTC instant. */
  updatedAt: string
}

/** Create/update body (`ExpenseRequest`). `amount` must be > 0. */
export interface ExpenseRequest {
  amount: number
  /** ISO calendar date `YYYY-MM-DD`. */
  date: string
  category: Category
}

/** Query params for `GET /api/expenses` (and the CSV export). All optional. */
export interface ExpenseQuery {
  /** Range start (inclusive), `YYYY-MM-DD`. Defaults server-side to current month start. */
  from?: string
  /** Range end (inclusive), `YYYY-MM-DD`. Defaults server-side to current month end. */
  to?: string
  category?: Category
  minAmount?: number
  maxAmount?: number
  /** Free-text search (reserved; currently matches category). */
  q?: string
  /** e.g. `date,desc` (default), `amount,asc`. */
  sort?: string
  /** 0-based page index (default 0). */
  page?: number
  /** Page size (default 50, max 200). */
  size?: number
}

/** Paginated list response (`PageResponse<Expense>`). */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  /** Echoed-back resolved sort string, e.g. `date,desc`. */
  sort: string
}

/** Shared date-range params for the summary endpoints. */
export interface SummaryQuery {
  from?: string
  to?: string
}

/** `GET /api/summary` body. */
export interface SummaryResponse {
  /** Resolved range start, `YYYY-MM-DD`. */
  from: string
  /** Resolved range end, `YYYY-MM-DD`. */
  to: string
  total: number
  count: number
  currency: string
}

/** A single slice of the category breakdown. */
export interface CategoryShare {
  category: Category
  total: number
  count: number
  /** Share of the grand total, percent rounded to two decimals server-side. */
  percent: number
}

/** `GET /api/summary/by-category` body. */
export interface CategorySummaryResponse {
  from: string
  to: string
  total: number
  /** One entry per category with spend in range, largest-first. */
  categories: CategoryShare[]
}

/** Trend bucket size. */
export type Granularity = 'day' | 'month'

/** Query params for `GET /api/summary/trend`. */
export interface TrendQuery extends SummaryQuery {
  granularity?: Granularity
}

/** A single trend bucket. `period` is `YYYY-MM-DD` (day) or `YYYY-MM` (month). */
export interface TrendPoint {
  period: string
  total: number
}

/** `GET /api/summary/trend` body. */
export interface TrendResponse {
  from: string
  to: string
  granularity: Granularity
  /** One point per non-empty bucket, ascending calendar order. */
  points: TrendPoint[]
}

/** Uniform API error body produced by the backend global exception handler. */
export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  fieldErrors?: { field: string; message: string }[]
}
