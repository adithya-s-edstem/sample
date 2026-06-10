import { useQuery } from '@tanstack/react-query'
import { getSummary, getSummaryByCategory, getSummaryTrend } from '../api/expenses'
import { queryKeys } from '../api/queryKeys'
import type { SummaryQuery, TrendQuery } from '../api/types'

/**
 * Headline total + count for a period (`GET /api/summary`). Omitted range params
 * default to the current month server-side.
 */
export function useSummary(query: SummaryQuery = {}) {
  return useQuery({
    queryKey: queryKeys.summary.totals(query),
    queryFn: () => getSummary(query),
  })
}

/** Per-category breakdown for the donut (`GET /api/summary/by-category`). */
export function useSummaryByCategory(query: SummaryQuery = {}) {
  return useQuery({
    queryKey: queryKeys.summary.byCategory(query),
    queryFn: () => getSummaryByCategory(query),
  })
}

/** Spending-over-time series for the trend chart (`GET /api/summary/trend`). */
export function useSummaryTrend(query: TrendQuery = {}) {
  return useQuery({
    queryKey: queryKeys.summary.trend(query),
    queryFn: () => getSummaryTrend(query),
  })
}
