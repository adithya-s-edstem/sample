import { useQuery } from '@tanstack/react-query'
import { listExpenses, getExpense } from '../api/expenses'
import { queryKeys } from '../api/queryKeys'
import type { ExpenseQuery } from '../api/types'

/**
 * Fetches a filtered/sorted/paginated page of expenses (`GET /api/expenses`).
 * Omitted range params let the backend default to the current month. The query
 * object is part of the cache key, so changing filters refetches and caches per
 * distinct param set.
 */
export function useExpenses(query: ExpenseQuery = {}) {
  return useQuery({
    queryKey: queryKeys.expenses.list(query),
    queryFn: () => listExpenses(query),
  })
}

/** Fetches a single expense by id (`GET /api/expenses/{id}`). Disabled until `id` is set. */
export function useExpense(id: string | undefined) {
  return useQuery({
    queryKey: queryKeys.expenses.detail(id ?? ''),
    queryFn: () => getExpense(id as string),
    enabled: id != null && id !== '',
  })
}
