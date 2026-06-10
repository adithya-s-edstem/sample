import { describe, it, expect } from 'vitest'
import { http, HttpResponse, delay } from 'msw'
import { screen, waitFor } from '@testing-library/react'
import TrendSection from './TrendSection'
import { server } from '../../test/msw/server'
import { sampleTrend } from '../../test/msw/handlers'
import { apiError, renderWithProviders } from '../../test/utils'
import type { TrendResponse } from '../../api/types'

// June 2026 is pinned so the resolved range feeding the query is deterministic.
const JUNE_2026 = new Date(2026, 5, 1)

describe('TrendSection — spending trend chart (P6-3)', () => {
  it('renders the bar chart from /summary/trend', async () => {
    const data: TrendResponse = {
      from: '2026-06-01',
      to: '2026-06-30',
      granularity: 'day',
      points: [
        { period: '2026-06-05', total: 1200 },
        { period: '2026-06-20', total: 2400 },
      ],
    }
    server.use(http.get('/api/summary/trend', () => HttpResponse.json(data)))

    renderWithProviders(<TrendSection />, { initialMonth: JUNE_2026 })

    await waitFor(() =>
      expect(screen.getByRole('img', { name: 'Spending over time' })).toBeInTheDocument(),
    )
    expect(screen.getAllByText('Jun 5').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Jun 20').length).toBeGreaterThan(0)
  })

  it('renders the empty state when the month has no spending', async () => {
    const empty: TrendResponse = {
      from: '2026-06-01',
      to: '2026-06-30',
      granularity: 'day',
      points: [],
    }
    server.use(http.get('/api/summary/trend', () => HttpResponse.json(empty)))

    renderWithProviders(<TrendSection />, { initialMonth: JUNE_2026 })

    await waitFor(() => expect(screen.getByText('No spending this month.')).toBeInTheDocument())
    expect(screen.queryByRole('img', { name: 'Spending over time' })).not.toBeInTheDocument()
  })

  it('shows the loading skeleton while /summary/trend is pending', () => {
    server.use(
      http.get('/api/summary/trend', async () => {
        await delay('infinite')
        return HttpResponse.json(sampleTrend)
      }),
    )

    renderWithProviders(<TrendSection />, { initialMonth: JUNE_2026 })
    expect(screen.getByLabelText('Loading spending trend')).toBeInTheDocument()
  })

  it('shows a graceful error + Retry when /summary/trend fails', async () => {
    server.use(
      http.get('/api/summary/trend', () =>
        HttpResponse.json(apiError({ status: 500 }), { status: 500 }),
      ),
    )

    renderWithProviders(<TrendSection />, { initialMonth: JUNE_2026 })
    await waitFor(() => expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument())
  })
})
