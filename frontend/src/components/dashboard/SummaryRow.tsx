import Card from '../layout/Card'

/*
 * Summary row shell: this-month total card + category-breakdown card laid out
 * 320px / 1fr like the wireframe. The total value and the donut/legend are
 * wired to live data in Phase 6 (P6-1, P6-2); here they are static placeholders
 * so the layout and theme can be reviewed.
 */
function SummaryRow() {
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
