/*
 * Empty state (P6-4) for the dashboard's expense list area, shown when the
 * selected month resolved successfully but has no expenses. Mirrors the
 * wireframe (docs/wireframes/empty.html .empty block): a centered receipt emoji,
 * "No expenses this month" heading, a one-line prompt, and a primary
 * "Add your first expense" call-to-action.
 *
 * This is the page-level empty prompt; the "This Month" card already reads ₹0 and
 * the donut shows its own "no spending" message, so together they reproduce the
 * full empty-state wireframe. The CTA opens the add modal in Phase 7 (P7-2); for
 * now it surfaces the action via an optional `onAddExpense` handler.
 */
type EmptyStateProps = {
  /** Opens the add-expense modal (wired in P7-2). */
  onAddExpense?: () => void
}

function EmptyState({ onAddExpense }: EmptyStateProps) {
  return (
    <div className="px-6 py-16 text-center text-muted">
      <div className="text-[40px]" aria-hidden="true">
        🧾
      </div>
      <h2 className="mb-1.5 mt-3.5 text-xl font-semibold text-ink">No expenses this month</h2>
      <p className="mb-[22px]">
        Start tracking where your money goes by adding your first expense.
      </p>
      <button
        type="button"
        onClick={onAddExpense}
        className="inline-flex cursor-pointer items-center gap-2 rounded-[10px] border-none bg-accent px-4 py-2.5 text-sm font-semibold text-white shadow-card"
      >
        <span className="text-base leading-none" aria-hidden="true">
          +
        </span>{' '}
        Add your first expense
      </button>
    </div>
  )
}

export default EmptyState
