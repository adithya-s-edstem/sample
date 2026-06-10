import Card from '../layout/Card'

/*
 * Expense list shell: section header with an Export CSV action and the table
 * header matching the wireframe. Rows, actions, and the empty state are wired
 * to live data in Phase 7 (P7-1); CSV export hooks up in P8-3.
 */
function ExpenseListSection() {
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
          <tr>
            <td className="px-3 py-6 text-center text-sm text-muted" colSpan={4}>
              Expense rows (Phase 7)
            </td>
          </tr>
        </tbody>
      </table>
    </Card>
  )
}

export default ExpenseListSection
