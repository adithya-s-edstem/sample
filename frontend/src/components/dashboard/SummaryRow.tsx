import Card from '../layout/Card'
import Skeleton from '../layout/Skeleton'
import ErrorState from '../layout/ErrorState'
import CategoryDonut from './CategoryDonut'
import { useMonth } from '../../context/monthContext'
import { useFilters } from '../../context/filterContext'
import { buildSummaryQuery } from '../../lib/filters'
import { useSummary, useSummaryByCategory } from '../../hooks'
import { expenseCountLabel, formatINR } from '../../lib/money'

/*
 * Summary row shell: this-month total card + category-breakdown card laid out
 * 320px / 1fr like the wireframe. Both cards are scoped to the selected month
 * (P5-3) via the `from`/`to` range from useMonth.
 *
 * P5-4 adds the loading/error states: each card independently shows the
 * wireframe skeleton (docs/wireframes/loading.html — sk-amount + sk-line for the
 * total, sk-donut + legend lines for the breakdown) while its query is pending,
 * and a graceful error + Retry on failure.
 *
 * P6-1 wires the "This Month" card to real /summary data: the formatted INR
 * total (lib/money) and a "{count} expenses · {month}" subtitle, matching the
 * dashboard wireframe. P6-2 wires the Category Breakdown card to a real Recharts
 * donut + legend (CategoryDonut) from /summary/by-category.
 *
 * P8-2 scopes both cards to the filter bar's selected scope: the date-range
 * filter (defaulting to the selected month) feeds the summary/by-category
 * queries via buildSummaryQuery, so changing the date range refines the totals
 * and donut in lockstep with the list. Per docs/api-contracts.md §3 the summary
 * endpoints only take a date range, so category/amount/search remain list-only.
 */
function SummaryRow() {
  const { range, label } = useMonth()
  const { filters } = useFilters()
  const scope = buildSummaryQuery(filters, range)
  const summary = useSummary(scope)
  const byCategory = useSummaryByCategory(scope)

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
            <div className="text-[40px] font-bold tracking-[-0.02em]">
              {formatINR(summary.data.total)}
            </div>
            <div className="mt-1.5 text-[13px] text-muted">
              {expenseCountLabel(summary.data.count)} · {label}
            </div>
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
          <CategoryDonut data={byCategory.data} />
        )}
      </Card>
    </div>
  )
}

export default SummaryRow
