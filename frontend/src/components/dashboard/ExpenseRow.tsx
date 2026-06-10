import type { Expense } from '../../api/types'
import { categoryColor, categoryLabel, categoryPillTextColor } from '../../lib/category'
import { formatINRExact } from '../../lib/money'
import { expenseDateLabel } from '../../lib/month'

/*
 * A single expense table row (P7-1), matching the dashboard wireframe
 * (docs/wireframes/dashboard.html): date as `d MMM yyyy`, a colored category
 * "pill", the right-aligned INR amount with two decimals, and edit/delete icon
 * actions. The pill tints itself with the stable per-category color (lib/category)
 * so it reads consistently with the donut legend.
 *
 * Money is display-only: `amount` is the exact two-decimal wire value
 * (server-side BigDecimal / NUMERIC(12,2)); we only format it here. The edit and
 * delete actions surface as accessible buttons and invoke optional callbacks:
 * `onEdit` opens the add/edit modal (P7-3) and `onDelete` opens the confirm
 * prompt (P7-4). Absent a handler the corresponding button is inert.
 */
type ExpenseRowProps = {
  expense: Expense
  onEdit?: (expense: Expense) => void
  onDelete?: (expense: Expense) => void
}

function ExpenseRow({ expense, onEdit, onDelete }: ExpenseRowProps) {
  const color = categoryColor(expense.category)
  const pillText = categoryPillTextColor(expense.category)
  const label = categoryLabel(expense.category)

  return (
    <tr className="border-b border-line last:border-b-0">
      <td className="px-3 py-3 text-sm text-ink">{expenseDateLabel(expense.date)}</td>
      <td className="px-3 py-3">
        <span
          className="inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold"
          style={{ background: `${color}1a`, color: pillText }}
        >
          {label}
        </span>
      </td>
      <td className="px-3 py-3 text-right text-sm font-semibold tabular-nums text-ink">
        {formatINRExact(expense.amount)}
      </td>
      <td className="whitespace-nowrap px-3 py-3 text-right">
        <button
          type="button"
          onClick={() => onEdit?.(expense)}
          aria-label={`Edit expense from ${expenseDateLabel(expense.date)}`}
          className="cursor-pointer rounded-md px-1.5 py-1 text-[15px] text-muted hover:bg-bg"
        >
          ✎
        </button>
        <button
          type="button"
          onClick={() => onDelete?.(expense)}
          aria-label={`Delete expense from ${expenseDateLabel(expense.date)}`}
          className="cursor-pointer rounded-md px-1.5 py-1 text-[15px] text-muted hover:bg-bg"
        >
          🗑
        </button>
      </td>
    </tr>
  )
}

export default ExpenseRow
