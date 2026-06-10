import Card from '../layout/Card'

/*
 * Spending-over-time shell. The Recharts bar/line chart from /summary/trend
 * lands in P6-3; this reserves the region at the wireframe's height.
 */
function TrendSection() {
  return (
    <Card title="Spending Over Time" className="mt-5">
      <div className="flex h-[220px] items-center justify-center text-sm text-muted">
        Spending trend chart (Phase 6)
      </div>
    </Card>
  )
}

export default TrendSection
