import ConfirmDialog from './ConfirmDialog'
import { useDeleteExpense } from '../../hooks'
import { getErrorMessage } from '../../api/client'
import { categoryLabel } from '../../lib/category'
import { formatINRExact } from '../../lib/money'
import { expenseDateLabel } from '../../lib/month'
import type { Expense } from '../../api/types'

/*
 * Stateful container for the delete-with-confirm flow (P7-4): it wraps the
 * presentational ConfirmDialog and connects a confirmed delete to the API
 * (docs/solution.md §4).
 *
 * On confirm it runs DELETE /api/expenses/{id} via useDeleteExpense, which on
 * success invalidates the list + every summary (total, donut, trend) so the
 * dashboard refreshes live (useExpenseMutations). The dialog closes only after
 * the delete succeeds; while it's in flight the actions are disabled (`busy`),
 * and a server-side failure is surfaced inline so the user can retry or cancel.
 */
type DeleteExpenseDialogProps = {
  /** The expense to delete; its details are echoed in the confirm copy. */
  expense: Expense
  /** Dismisses the dialog (Cancel/overlay/Escape, or after a successful delete). */
  onClose: () => void
}

function DeleteExpenseDialog({ expense, onClose }: DeleteExpenseDialogProps) {
  const remove = useDeleteExpense()

  function handleConfirm() {
    remove.mutate(expense.id, { onSuccess: () => onClose() })
  }

  return (
    <ConfirmDialog
      title="Delete expense?"
      message={`This permanently removes the ${formatINRExact(expense.amount)} ${categoryLabel(
        expense.category,
      )} expense from ${expenseDateLabel(expense.date)}. This can't be undone.`}
      confirmLabel="Delete"
      onConfirm={handleConfirm}
      onClose={onClose}
      busy={remove.isPending}
      error={remove.isError ? getErrorMessage(remove.error) : undefined}
    />
  )
}

export default DeleteExpenseDialog
