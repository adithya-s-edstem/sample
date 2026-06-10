import Card from '../layout/Card'
import Skeleton from '../layout/Skeleton'
import ErrorState from '../layout/ErrorState'
import TrendChart from './TrendChart'
import { useMonth } from '../../context/monthContext'
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
 */
function TrendSection() {
  const { range } = useMonth()
  const trend = useSummaryTrend(range)

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
