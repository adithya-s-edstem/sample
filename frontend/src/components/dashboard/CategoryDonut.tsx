import { Cell, Pie, PieChart } from 'recharts'
import type { CategorySummaryResponse } from '../../api/types'
import { categoryColor, categoryLabel } from '../../lib/category'
import { formatINR } from '../../lib/money'

/*
 * Category donut + legend (P6-2), rendering /summary/by-category as the donut in
 * the "Category Breakdown" card. The chart mirrors the wireframe
 * (docs/wireframes/dashboard.html): a fixed 160px Recharts donut on the left and
 * a legend on the right, one row per category with a colored dot, Title-case
 * label, and "₹amount · percent%" — slices/legend largest-first (the backend
 * already orders categories that way) with a stable color per category.
 *
 * Money is display-only here: totals/percents are computed server-side as exact
 * decimals (docs/api-contracts.md) and we only format them, so no float drift.
 * A fixed-size PieChart (not ResponsiveContainer) is used so it renders under
 * jsdom in tests too.
 */
type CategoryDonutProps = {
  data: CategorySummaryResponse
}

const SIZE = 160
const RADIUS = SIZE / 2

function CategoryDonut({ data }: CategoryDonutProps) {
  // Empty/zero state: no spend in the period → no slices to draw (P6-4 owns the
  // page-level empty state; this keeps the card from rendering a broken donut).
  if (data.categories.length === 0) {
    return <p className="text-sm text-muted">No spending this month.</p>
  }

  return (
    <div className="flex flex-wrap items-center gap-7">
      <PieChart width={SIZE} height={SIZE} role="img" aria-label="Spending by category">
        <Pie
          data={data.categories}
          dataKey="total"
          nameKey="category"
          cx="50%"
          cy="50%"
          innerRadius={RADIUS - 26}
          outerRadius={RADIUS}
          startAngle={90}
          endAngle={-270}
          paddingAngle={0}
          stroke="none"
          isAnimationActive={false}
        >
          {data.categories.map((slice) => (
            <Cell key={slice.category} fill={categoryColor(slice.category)} />
          ))}
        </Pie>
      </PieChart>

      <ul className="flex-1 space-y-2.5 text-sm">
        {data.categories.map((slice) => (
          <li key={slice.category} className="flex items-center gap-2.5">
            <span
              className="size-[11px] shrink-0 rounded-[3px]"
              style={{ background: categoryColor(slice.category) }}
              aria-hidden="true"
            />
            <span>{categoryLabel(slice.category)}</span>
            <span className="ml-auto text-muted">
              {formatINR(slice.total)} · {slice.percent}%
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default CategoryDonut
