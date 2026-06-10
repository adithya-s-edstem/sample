import { describe, it, expect } from 'vitest'
import { http, HttpResponse, delay } from 'msw'
import { screen, waitFor } from '@testing-library/react'
import SummaryRow from './SummaryRow'
import { server } from '../../test/msw/server'
import { sampleByCategory, sampleSummary } from '../../test/msw/handlers'
import { apiError, renderWithProviders } from '../../test/utils'
import type { CategorySummaryResponse, SummaryResponse } from '../../api/types'

// June 2026 is pinned so the "· June 2026" subtitle is deterministic.
const JUNE_2026 = new Date(2026, 5, 1)

describe('SummaryRow — "This Month" total card (P6-1)', () => {
  it('renders the formatted total and count · month subtitle from /summary', async () => {
    renderWithProviders(<SummaryRow />, { initialMonth: JUNE_2026 })

    // total: sampleSummary.total = 1234.56 → ₹1,234.56 (paise preserved)
    await waitFor(() => expect(screen.getByText('₹1,234.56')).toBeInTheDocument())
    expect(screen.getByText('1 expense · June 2026')).toBeInTheDocument()
  })

  it('renders a clean zero/empty total when the month has no expenses', async () => {
    const empty: SummaryResponse = {
      from: '2026-06-01',
      to: '2026-06-30',
      total: 0,
      count: 0,
      currency: 'INR',
    }
    server.use(http.get('/api/summary', () => HttpResponse.json(empty)))

    renderWithProviders(<SummaryRow />, { initialMonth: JUNE_2026 })

    await waitFor(() => expect(screen.getByText('₹0')).toBeInTheDocument())
    expect(screen.getByText('0 expenses · June 2026')).toBeInTheDocument()
  })

  it('shows the loading skeleton while /summary is pending', () => {
    server.use(
      http.get('/api/summary', async () => {
        await delay('infinite')
        return HttpResponse.json(sampleSummary)
      }),
    )

    renderWithProviders(<SummaryRow />, { initialMonth: JUNE_2026 })
    expect(screen.getByLabelText("Loading this month's total")).toBeInTheDocument()
  })

  it('shows a graceful error + Retry when /summary fails', async () => {
    server.use(
      http.get('/api/summary', () => HttpResponse.json(apiError({ status: 500 }), { status: 500 })),
    )

    renderWithProviders(<SummaryRow />, { initialMonth: JUNE_2026 })
    await waitFor(() => expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument())
  })
})

describe('SummaryRow — "Category Breakdown" donut (P6-2)', () => {
  it('renders the donut + legend from /summary/by-category', async () => {
    const data: CategorySummaryResponse = {
      from: '2026-06-01',
      to: '2026-06-30',
      total: 17200,
      categories: [
        { category: 'RENT', total: 12000, count: 1, percent: 69.77 },
        { category: 'GROCERIES', total: 5200, count: 2, percent: 30.23 },
      ],
    }
    server.use(http.get('/api/summary/by-category', () => HttpResponse.json(data)))

    renderWithProviders(<SummaryRow />, { initialMonth: JUNE_2026 })

    await waitFor(() =>
      expect(screen.getByRole('img', { name: 'Spending by category' })).toBeInTheDocument(),
    )
    expect(screen.getByText('Rent')).toBeInTheDocument()
    expect(screen.getByText('₹12,000 · 69.77%')).toBeInTheDocument()
    expect(screen.getByText('Groceries')).toBeInTheDocument()
    expect(screen.getByText('₹5,200 · 30.23%')).toBeInTheDocument()
  })

  it('shows the donut skeleton while /summary/by-category is pending', () => {
    server.use(
      http.get('/api/summary/by-category', async () => {
        await delay('infinite')
        return HttpResponse.json(sampleByCategory)
      }),
    )

    renderWithProviders(<SummaryRow />, { initialMonth: JUNE_2026 })
    expect(screen.getByLabelText('Loading category breakdown')).toBeInTheDocument()
  })

  it('shows a graceful error + Retry when /summary/by-category fails', async () => {
    server.use(
      http.get('/api/summary/by-category', () =>
        HttpResponse.json(apiError({ status: 500 }), { status: 500 }),
      ),
    )

    renderWithProviders(<SummaryRow />, { initialMonth: JUNE_2026 })
    await waitFor(() => expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument())
  })
})
