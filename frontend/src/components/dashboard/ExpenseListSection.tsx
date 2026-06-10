import Card from '../layout/Card'
import Skeleton from '../layout/Skeleton'
import ErrorState from '../layout/ErrorState'
import EmptyState from './EmptyState'
import ExpenseRow from './ExpenseRow'
import { useMonth } from '../../context/monthContext'
import { useFilters } from '../../context/filterContext'
import { buildExpenseQuery } from '../../lib/filters'
import { useExpenses } from '../../hooks'
import type { Expense } from '../../api/types'

/*
 * Expense list shell: section header with an Export CSV action and the table
 * header matching the wireframe. The list query is scoped to the selected month
 * (P5-3) via the `from`/`to` range from useMonth, and further refined by the
 * filter bar (P8-1): the FilterProvider state (date range, category, amount
 * range, search) is folded into the `GET /api/expenses` query params.
 *
 * P5-4 adds the loading/error states: while the query is pending, the table body
 * shows the wireframe's row skeletons (docs/wireframes/loading.html — five
 * sk-row blocks); on failure the body shows a graceful error + Retry.
 *
 * P6-4 adds the empty state: when the query succeeds but the selected month has
 * no expenses, the whole card becomes the friendly "No expenses this month"
 * prompt with an "Add your first expense" CTA (docs/wireframes/empty.html) — the
 * table header and Export action are hidden since there is nothing to list or
 * export.
 *
 * P7-1 renders the real rows from the page content: each expense becomes an
 * ExpenseRow (date, category pill, amount, edit/delete actions). P7-3 wires the
 * edit action and the empty-state CTA to the add/edit modal via `onEditExpense` /
 * `onAddExpense` (state lives in App); P7-4 wires each row's delete action to the
 * confirm prompt via `onDeleteExpense`. CSV export (P8-3) follows.
 */
type ExpenseListSectionProps = {
  /** Opens the add-expense modal (header / empty-state CTA share this). */
  onAddExpense?: () => void
  /** Opens the edit modal pre-filled from the given expense. */
  onEditExpense?: (expense: Expense) => void
  /** Opens the delete confirm prompt for the given expense. */
  onDeleteExpense?: (expense: Expense) => void
}

function ExpenseListSection({
  onAddExpense,
  onEditExpense,
  onDeleteExpense,
}: ExpenseListSectionProps) {
  const { range } = useMonth()
  const { filters } = useFilters()
  // P8-1: the filter bar folds into the list query — date range (defaulting to
  // the selected month), category, amount range, and search.
  const expenses = useExpenses(buildExpenseQuery(filters, range))

  // Successful fetch with no rows for the month → page-level empty prompt.
  if (expenses.isSuccess && expenses.data.content.length === 0) {
    return (
      <Card>
        <EmptyState onAddExpense={onAddExpense} />
      </Card>
    )
  }

  return (
    <Card>
      <div className="mb-3 flex items-center justify-between">
        <p className="text-[13px] font-semibold uppercase tracking-[0.04em] text-muted">Expenses</p>
        <button
          type="button"
          className="inline-flex cursor-pointer items-center gap-2 rounded-[10px] border border-line bg-card px-4 py-2.5 text-sm font-medium text-ink shadow-card"
        >
          ⬇ Export CSV
        </button>
      </div>

      <table className="w-full border-collapse">
        <thead>
          <tr>
            <th className="border-b border-line px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-[0.04em] text-muted">
              Date
            </th>
            <th className="border-b border-line px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-[0.04em] text-muted">
              Category
            </th>
            <th className="border-b border-line px-3 py-2.5 text-right text-xs font-semibold uppercase tracking-[0.04em] text-muted">
              Amount
            </th>
            <th className="border-b border-line px-3 py-2.5 text-right text-xs font-semibold uppercase tracking-[0.04em] text-muted">
              Actions
            </th>
          </tr>
        </thead>
        <tbody>
          {expenses.isPending ? (
            Array.from({ length: 5 }).map((_, i) => (
              <tr key={i} aria-busy="true">
                <td className="px-3 py-4" colSpan={4}>
                  <Skeleton className="h-[18px] w-full" />
                </td>
              </tr>
            ))
          ) : expenses.isError ? (
            <tr>
              <td className="px-3 py-2" colSpan={4}>
                <ErrorState
                  error={expenses.error}
                  onRetry={() => void expenses.refetch()}
                  title="Couldn't load expenses"
                />
              </td>
            </tr>
          ) : (
            expenses.data.content.map((expense) => (
              <ExpenseRow
                key={expense.id}
                expense={expense}
                onEdit={onEditExpense}
                onDelete={onDeleteExpense}
              />
            ))
          )}
        </tbody>
      </table>
    </Card>
  )
}

export default ExpenseListSection
