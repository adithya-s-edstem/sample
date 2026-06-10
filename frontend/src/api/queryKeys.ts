import type { ExpenseQuery, SummaryQuery, TrendQuery } from './types'

/*
 * Centralized TanStack Query key factory. Hooks build keys from these so cache
 * reads/writes and invalidations stay consistent. Keys are hierarchical: the
 * broad `expenses` / `summary` roots let mutations invalidate every dependent
 * query (list + all summaries) after a create/update/delete.
 */
export const queryKeys = {
  expenses: {
    all: ['expenses'] as const,
    list: (query: ExpenseQuery) => ['expenses', 'list', query] as const,
    detail: (id: string) => ['expenses', 'detail', id] as const,
  },
  summary: {
    all: ['summary'] as const,
    totals: (query: SummaryQuery) => ['summary', 'totals', query] as const,
    byCategory: (query: SummaryQuery) => ['summary', 'by-category', query] as const,
    trend: (query: TrendQuery) => ['summary', 'trend', query] as const,
  },
  categories: ['categories'] as const,
}
