import ExpenseModal from './ExpenseModal'
import { useCreateExpense, useUpdateExpense } from '../../hooks'
import { getErrorMessage } from '../../api/client'
import type { Expense, ExpenseRequest } from '../../api/types'

/*
 * Stateful container for the Add/Edit flow (P7-3): it wraps the presentational
 * ExpenseModal and connects a validated Save to the API.
 *
 * - Add mode (no `expense`)  → POST /api/expenses via useCreateExpense.
 * - Edit mode (`expense`)    → PUT  /api/expenses/{id} via useUpdateExpense.
 *
 * On success the mutation invalidates the list and every summary (total, donut,
 * trend) — and the expense's detail in edit mode — so the dashboard refreshes
 * live (useExpenseMutations). The modal closes only after the write succeeds;
 * while it's in flight the form is disabled (`submitting`), and a server-side
 * failure (e.g. a 400 the client validation didn't catch) is surfaced inline as
 * a form-level banner with the backend's uniform `message`. Money is never
 * touched here: the exact two-decimal ExpenseRequest from the modal is sent
 * straight through.
 */
type ExpenseFormModalProps = {
  /** When set, edit that expense; otherwise the modal is in add mode. */
  expense?: Expense
  /** Closes the modal (Cancel/overlay/Escape, or after a successful save). */
  onClose: () => void
}

function ExpenseFormModal({ expense, onClose }: ExpenseFormModalProps) {
  const create = useCreateExpense()
  const update = useUpdateExpense()
  const mutation = expense ? update : create

  function handleSubmit(body: ExpenseRequest) {
    if (expense) {
      update.mutate({ id: expense.id, body }, { onSuccess: () => onClose() })
    } else {
      create.mutate(body, { onSuccess: () => onClose() })
    }
  }

  return (
    <ExpenseModal
      expense={expense}
      onSubmit={handleSubmit}
      onClose={onClose}
      submitting={mutation.isPending}
      serverError={mutation.isError ? getErrorMessage(mutation.error) : undefined}
    />
  )
}

export default ExpenseFormModal
