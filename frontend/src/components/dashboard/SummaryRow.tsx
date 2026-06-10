import Card from '../layout/Card'
import { useMonth } from '../../context/monthContext'
import { useSummary, useSummaryByCategory } from '../../hooks'

/*
 * Summary row shell: this-month total card + category-breakdown card laid out
 * 320px / 1fr like the wireframe. P5-3 scopes the summary/by-category queries to
 * the selected month (the `from`/`to` range from useMonth feeds the hooks); the
 * total value and the donut/legend are rendered from this data in Phase 6
 * (P6-1, P6-2). Here the layout/theme remain placeholders.
 */
function SummaryRow() {
  const { range } = useMonth()
  // Queries are scoped to the selected month; rendering of the values lands in P6.
  useSummary(range)
  useSummaryByCategory(range)

  return (
    <div className="grid gap-5 md:grid-cols-[320px_1fr]">
      <Card title="This Month">
        <div className="text-[40px] font-bold tracking-[-0.02em]">₹0</div>
        <div className="mt-1.5 text-[13px] text-muted">No expenses yet</div>
      </Card>

      <Card title="Category Breakdown">
        <div className="flex items-center gap-7">
          <div className="size-[150px] shrink-0 rounded-full border-[26px] border-line" />
          <p className="text-sm text-muted">Category donut and legend (Phase 6)</p>
        </div>
      </Card>
    </div>
  )
}

export default SummaryRow
