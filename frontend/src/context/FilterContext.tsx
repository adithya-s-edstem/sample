/*
 * Filter-state provider (P8-1). Holds the filter-bar inputs (date range,
 * category, amount range, search) as a single source of truth so the filter bar
 * can drive the expense list query. The selected month (MonthContext) still
 * supplies the default date range; an explicit date-range filter overrides it.
 *
 * The context object and the `useFilters` hook live in ./filterContext (a non-
 * component module) so this file exports only a component.
 */
import { useCallback, useMemo, useState, type ReactNode } from 'react'
import {
  EMPTY_FILTERS,
  FilterContext,
  type FilterContextValue,
  type FilterState,
} from './filterContext'

interface FilterProviderProps {
  children: ReactNode
  /** Optional initial filters (mainly for tests); defaults to no filters. */
  initialFilters?: Partial<FilterState>
}

export function FilterProvider({ children, initialFilters }: FilterProviderProps) {
  const [filters, setFiltersState] = useState<FilterState>(() => ({
    ...EMPTY_FILTERS,
    ...initialFilters,
  }))

  const setFilters = useCallback(
    (patch: Partial<FilterState>) => setFiltersState((prev) => ({ ...prev, ...patch })),
    [],
  )
  const clearFilters = useCallback(() => setFiltersState(EMPTY_FILTERS), [])

  const value = useMemo<FilterContextValue>(() => {
    const hasActiveFilters = (Object.keys(filters) as (keyof FilterState)[]).some(
      (key) => filters[key] !== EMPTY_FILTERS[key],
    )
    return { filters, setFilters, clearFilters, hasActiveFilters }
  }, [filters, setFilters, clearFilters])

  return <FilterContext.Provider value={value}>{children}</FilterContext.Provider>
}
