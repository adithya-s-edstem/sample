import Card from '../layout/Card'
import { useMonth } from '../../context/monthContext'
import { useSummaryTrend } from '../../hooks'

/*
 * Spending-over-time shell. P5-3 scopes the trend query to the selected month
 * (the `from`/`to` range from useMonth feeds the hook; the backend defaults
 * granularity to `day` within a single month). The Recharts bar/line chart that
 * renders this series lands in P6-3; this reserves the region at the wireframe's
 * height.
 */
function TrendSection() {
  const { range } = useMonth()
  // Query scoped to the selected month; chart rendering lands in P6-3.
  useSummaryTrend(range)

  return (
    <Card title="Spending Over Time" className="mt-5">
      <div className="flex h-[220px] items-center justify-center text-sm text-muted">
        Spending trend chart (Phase 6)
      </div>
    </Card>
  )
}

export default TrendSection
