import Card from '../layout/Card'
import Skeleton from '../layout/Skeleton'
import ErrorState from '../layout/ErrorState'
import { useMonth } from '../../context/monthContext'
import { useExpenses } from '../../hooks'

/*
 * Expense list shell: section header with an Export CSV action and the table
 * header matching the wireframe. The list query is scoped to the selected month
 * (P5-3) via the `from`/`to` range from useMonth.
 *
 * P5-4 adds the loading/error states: while the query is pending, the table body
 * shows the wireframe's row skeletons (docs/wireframes/loading.html — five
 * sk-row blocks); on failure the body shows a graceful error + Retry. Rows,
 * actions, and the empty state are rendered from this data in Phase 7 (P7-1);
 * CSV export hooks up in P8-3.
 */
function ExpenseListSection() {
  const { range } = useMonth()
  const expenses = useExpenses(range)

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
            <tr>
              <td className="px-3 py-6 text-center text-sm text-muted" colSpan={4}>
                Expense rows (Phase 7)
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </Card>
  )
}

export default ExpenseListSection
