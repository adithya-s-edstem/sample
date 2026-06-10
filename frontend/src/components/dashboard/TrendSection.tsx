import Card from '../layout/Card'
import Skeleton from '../layout/Skeleton'
import ErrorState from '../layout/ErrorState'
import TrendChart from './TrendChart'
import { useMonth } from '../../context/monthContext'
import { useFilters } from '../../context/filterContext'
import { buildSummaryQuery } from '../../lib/filters'
import { useSummaryTrend } from '../../hooks'

/*
 * Spending-over-time shell, scoped to the selected month (P5-3) via the
 * `from`/`to` range from useMonth (the backend defaults granularity to `day`
 * within a single month).
 *
 * P5-4 adds the loading/error states: the wireframe's full-height bar skeleton
 * (docs/wireframes/loading.html — sk-bar, 220px) while the query is pending, and
 * a graceful error + Retry on failure. P6-3 wires the success branch to a real
 * Recharts bar chart (TrendChart) from /summary/trend.
 *
 * P8-2 scopes the trend to the filter bar's selected scope: the date-range
 * filter (defaulting to the selected month) feeds the query via
 * buildSummaryQuery, so changing the date range refines the chart in lockstep
 * with the list. Per docs/api-contracts.md §3 the trend endpoint only takes a
 * date range (+ granularity, which the backend derives), so the category/amount/
 * search filters remain list-only.
 */
function TrendSection() {
  const { range } = useMonth()
  const { filters } = useFilters()
  const trend = useSummaryTrend(buildSummaryQuery(filters, range))

  return (
    <Card title="Spending Over Time" className="mt-5">
      {trend.isPending ? (
        <div aria-busy="true" aria-label="Loading spending trend">
          <Skeleton className="h-[220px] w-full" />
        </div>
      ) : trend.isError ? (
        <ErrorState error={trend.error} onRetry={() => void trend.refetch()} />
      ) : (
        <TrendChart data={trend.data} />
      )}
    </Card>
  )
}

export default TrendSection
