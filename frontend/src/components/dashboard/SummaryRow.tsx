import Card from '../layout/Card'
import Skeleton from '../layout/Skeleton'
import ErrorState from '../layout/ErrorState'
import { useMonth } from '../../context/monthContext'
import { useSummary, useSummaryByCategory } from '../../hooks'

/*
 * Summary row shell: this-month total card + category-breakdown card laid out
 * 320px / 1fr like the wireframe. Both cards are scoped to the selected month
 * (P5-3) via the `from`/`to` range from useMonth.
 *
 * P5-4 adds the loading/error states: each card independently shows the
 * wireframe skeleton (docs/wireframes/loading.html — sk-amount + sk-line for the
 * total, sk-donut + legend lines for the breakdown) while its query is pending,
 * and a graceful error + Retry on failure. The success rendering (the total
 * value and the donut/legend) lands in Phase 6 (P6-1, P6-2); placeholders remain
 * here.
 */
function SummaryRow() {
  const { range } = useMonth()
  const summary = useSummary(range)
  const byCategory = useSummaryByCategory(range)

  return (
    <div className="grid gap-5 md:grid-cols-[320px_1fr]">
      <Card title="This Month">
        {summary.isPending ? (
          <div aria-busy="true" aria-label="Loading this month's total">
            <Skeleton className="h-10 w-3/5" />
            <Skeleton className="mt-3.5 h-3.5 w-2/5" />
          </div>
        ) : summary.isError ? (
          <ErrorState error={summary.error} onRetry={() => void summary.refetch()} />
        ) : (
          <>
            <div className="text-[40px] font-bold tracking-[-0.02em]">₹0</div>
            <div className="mt-1.5 text-[13px] text-muted">No expenses yet</div>
          </>
        )}
      </Card>

      <Card title="Category Breakdown">
        {byCategory.isPending ? (
          <div
            className="flex items-center gap-7"
            aria-busy="true"
            aria-label="Loading category breakdown"
          >
            <Skeleton className="size-[150px] shrink-0 rounded-full" />
            <div className="flex-1 space-y-2.5">
              <Skeleton className="h-3.5 w-4/5" />
              <Skeleton className="h-3.5 w-[65%]" />
              <Skeleton className="h-3.5 w-[72%]" />
              <Skeleton className="h-3.5 w-[55%]" />
            </div>
          </div>
        ) : byCategory.isError ? (
          <ErrorState error={byCategory.error} onRetry={() => void byCategory.refetch()} />
        ) : (
          <div className="flex items-center gap-7">
            <div className="size-[150px] shrink-0 rounded-full border-[26px] border-line" />
            <p className="text-sm text-muted">Category donut and legend (Phase 6)</p>
          </div>
        )}
      </Card>
    </div>
  )
}

export default SummaryRow
