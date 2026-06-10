/*
 * Non-component module for the filter-state context (P8-1): the React context
 * object, its value/state types, and the `useFilters` accessor. Kept separate
 * from the provider component (FilterContext.tsx) so the provider file only
 * exports a component (Fast Refresh / react-refresh requirement).
 *
 * The filter bar (date range, category, amount range, search) drives the
 * `GET /api/expenses` list query. The selected month (MonthContext) supplies the
 * default `from`/`to` range; an explicit date-range filter, when set, overrides
 * it. Category / amount / search map straight onto the list query params
 * (docs/api-contracts.md). P8-2 will reuse this state to scope the summaries.
 */
import { createContext, useContext } from 'react'
import type { Category } from '../api/types'

/**
 * Raw, UI-shaped filter state. All fields optional / empty when unset. Amounts
 * and the date range are kept as strings (the raw `<input>` values) and coerced
 * into the typed `ExpenseQuery` at the edge; `''` means "no filter".
 */
export interface FilterState {
  /** Explicit range start `YYYY-MM-DD`; overrides the month range when set. */
  from: string
  /** Explicit range end `YYYY-MM-DD`; overrides the month range when set. */
  to: string
  /** Single-category filter, or `''` for all categories. */
  category: Category | ''
  /** Minimum amount (raw input string), or `''`. */
  minAmount: string
  /** Maximum amount (raw input string), or `''`. */
  maxAmount: string
  /** Free-text search, or `''`. */
  q: string
}

/** The empty filter set — no filters applied. */
export const EMPTY_FILTERS: FilterState = {
  from: '',
  to: '',
  category: '',
  minAmount: '',
  maxAmount: '',
  q: '',
}

export interface FilterContextValue {
  /** Current raw filter state (the controlled-input values). */
  filters: FilterState
  /** Patch one or more filter fields. */
  setFilters: (patch: Partial<FilterState>) => void
  /** Reset every filter back to its empty value. */
  clearFilters: () => void
  /** True when any filter field is set (used to surface a "clear" affordance). */
  hasActiveFilters: boolean
}

export const FilterContext = createContext<FilterContextValue | undefined>(undefined)

/** Read the filter state. Must be used inside a `FilterProvider`. */
export function useFilters(): FilterContextValue {
  const ctx = useContext(FilterContext)
  if (ctx === undefined) {
    throw new Error('useFilters must be used within a FilterProvider')
  }
  return ctx
}
