import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import TrendChart from './TrendChart'
import type { TrendResponse } from '../../api/types'

/*
 * TrendChart renders /summary/trend as a bar chart (P6-3). The SVG bars render
 * under jsdom because the BarChart is fixed-size, but their geometry isn't worth
 * asserting; we verify the labelled chart, the per-bucket x-axis labels (day vs.
 * month granularity), and the empty state.
 */
const daily: TrendResponse = {
  from: '2026-06-01',
  to: '2026-06-30',
  granularity: 'day',
  points: [
    { period: '2026-06-05', total: 1200 },
    { period: '2026-06-12', total: 800.5 },
    { period: '2026-06-20', total: 2400 },
  ],
}

describe('TrendChart (P6-3)', () => {
  it('renders a labelled bar chart', () => {
    render(<TrendChart data={daily} />)
    expect(screen.getByRole('img', { name: 'Spending over time' })).toBeInTheDocument()
  })

  it('renders a `MMM d` x-axis label per day bucket', () => {
    render(<TrendChart data={daily} />)
    // Recharts also mirrors the last-measured label into a hidden measurement
    // span, so a tick label can appear more than once; getAllByText tolerates that.
    expect(screen.getAllByText('Jun 5').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Jun 12').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Jun 20').length).toBeGreaterThan(0)
  })

  it('renders `MMM` x-axis labels for month granularity', () => {
    const monthly: TrendResponse = {
      from: '2026-01-01',
      to: '2026-03-31',
      granularity: 'month',
      points: [
        { period: '2026-01', total: 21000 },
        { period: '2026-02', total: 18750 },
        { period: '2026-03', total: 23100 },
      ],
    }
    render(<TrendChart data={monthly} />)
    expect(screen.getAllByText('Jan').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Feb').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Mar').length).toBeGreaterThan(0)
  })

  it('shows an empty message when the period has no spending', () => {
    const empty: TrendResponse = {
      from: '2026-06-01',
      to: '2026-06-30',
      granularity: 'day',
      points: [],
    }
    render(<TrendChart data={empty} />)
    expect(screen.getByText('No spending this month.')).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })

  it('renders the chart (not the empty message) when buckets exist but every total is zero', () => {
    // Zero state vs. empty state: the period has buckets, just no spend in them
    // (a real /summary/trend payload for a fully-zero range). The chart still
    // draws — labelled axis + a bar per bucket at zero height — rather than
    // collapsing to the "No spending" message, which is reserved for `points: []`.
    const allZero: TrendResponse = {
      from: '2026-06-01',
      to: '2026-06-30',
      granularity: 'day',
      points: [
        { period: '2026-06-05', total: 0 },
        { period: '2026-06-12', total: 0 },
      ],
    }
    render(<TrendChart data={allZero} />)
    expect(screen.getByRole('img', { name: 'Spending over time' })).toBeInTheDocument()
    expect(screen.queryByText('No spending this month.')).not.toBeInTheDocument()
    expect(screen.getAllByText('Jun 5').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Jun 12').length).toBeGreaterThan(0)
  })
})
