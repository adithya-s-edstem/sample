import { Bar, BarChart, CartesianGrid, XAxis } from 'recharts'
import type { TrendResponse } from '../../api/types'
import { trendPointLabel } from '../../lib/month'

/*
 * Spending-over-time bar chart (P6-3), rendering /summary/trend as the bars in the
 * "Spending Over Time" card. The chart mirrors the wireframe
 * (docs/wireframes/dashboard.html): a fixed 220px-tall Recharts BarChart with
 * faint gridlines, indigo bars, and short x-axis labels per bucket (`Jun 5` for
 * day granularity, `Jun` for month). Buckets arrive in ascending calendar order
 * from the backend, so the bars read left-to-right oldest→newest.
 *
 * Money is display-only here: bucket totals are exact two-decimal values computed
 * server-side (docs/api-contracts.md) and we only chart/format them, so no float
 * drift is introduced. A fixed-size BarChart (not ResponsiveContainer) is used so
 * it renders deterministically under jsdom in tests too.
 */
type TrendChartProps = {
  data: TrendResponse
}

const WIDTH = 720
const HEIGHT = 220
const BAR_COLOR = '#4f46e5'

function TrendChart({ data }: TrendChartProps) {
  // Empty/zero state: no spend in the period → no bars to draw (P6-4 owns the
  // page-level empty state; this keeps the card from rendering an empty axis).
  if (data.points.length === 0) {
    return <p className="text-sm text-muted">No spending this month.</p>
  }

  const chartData = data.points.map((point) => ({
    label: trendPointLabel(point.period, data.granularity),
    total: point.total,
  }))

  return (
    <BarChart
      width={WIDTH}
      height={HEIGHT}
      data={chartData}
      margin={{ top: 8, right: 8, bottom: 0, left: 8 }}
      role="img"
      aria-label="Spending over time"
    >
      <CartesianGrid stroke="#eef0f3" vertical={false} />
      <XAxis
        dataKey="label"
        tickLine={false}
        axisLine={false}
        tick={{ fill: '#6b7280', fontSize: 13 }}
      />
      <Bar dataKey="total" fill={BAR_COLOR} radius={[6, 6, 0, 0]} isAnimationActive={false} />
    </BarChart>
  )
}

export default TrendChart
