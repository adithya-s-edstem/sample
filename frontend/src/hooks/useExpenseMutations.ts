import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createExpense, updateExpense, deleteExpense } from '../api/expenses'
import { queryKeys } from '../api/queryKeys'
import type { ExpenseRequest } from '../api/types'

/*
 * Mutation hooks for create/update/delete. Any successful write invalidates the
 * `expenses` and `summary` roots so the list and every summary (total, donut,
 * trend) refetch and the dashboard stays consistent. Update/delete additionally
 * invalidate that expense's detail key.
 */

/** Invalidates the list + all summaries after a write. */
function useInvalidateExpenseData() {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.expenses.all })
    queryClient.invalidateQueries({ queryKey: queryKeys.summary.all })
  }
}

/** `POST /api/expenses` — create an expense. */
export function useCreateExpense() {
  const invalidate = useInvalidateExpenseData()
  return useMutation({
    mutationFn: (body: ExpenseRequest) => createExpense(body),
    onSuccess: () => invalidate(),
  })
}

/** `PUT /api/expenses/{id}` — full update of an expense. */
export function useUpdateExpense() {
  const invalidate = useInvalidateExpenseData()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: ExpenseRequest }) => updateExpense(id, body),
    onSuccess: (_data, { id }) => {
      invalidate()
      queryClient.invalidateQueries({ queryKey: queryKeys.expenses.detail(id) })
    },
  })
}

/** `DELETE /api/expenses/{id}` — delete an expense. */
export function useDeleteExpense() {
  const invalidate = useInvalidateExpenseData()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deleteExpense(id),
    onSuccess: (_data, id) => {
      invalidate()
      queryClient.invalidateQueries({ queryKey: queryKeys.expenses.detail(id) })
    },
  })
}
