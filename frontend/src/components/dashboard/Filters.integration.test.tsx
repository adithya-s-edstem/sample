import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { screen, fireEvent, waitFor } from '@testing-library/react'
import FilterBar from './FilterBar'
import SummaryRow from './SummaryRow'
import TrendSection from './TrendSection'
import ExpenseListSection from './ExpenseListSection'
import { server } from '../../test/msw/server'
import {
  sampleByCategory,
  sampleExpensePage,
  sampleSummary,
  sampleTrend,
} from '../../test/msw/handlers'
import { renderWithProviders } from '../../test/utils'

/*
 * P8-4 — integrated component test: changing a filter in the FilterBar updates
 * the query params of every dependent region at once. This is the phase-8 Exit
 * proof — "filtering/search refine list + charts; CSV downloads the filtered
 * set" — exercised through real user interaction (not seeded initialFilters):
 * the FilterBar, the SummaryRow + TrendSection charts, and the ExpenseListSection
 * (list + Export CSV) all read the one shared FilterProvider, so a single filter
 * change must fan out to the list query, the three summary/chart endpoints, and
 * the export URL together.
 *
 * Per docs/api-contracts.md §3 the summary endpoints only accept a date range, so
 * category/amount/search refine the list (and CSV) but NOT the charts — that
 * split is asserted here too.
 */

// June 2026 is pinned so the resolved default range is deterministic.
const JUNE_2026 = new Date(2026, 5, 1)

/** Captures the querystring of the most recent request to each endpoint. */
let listParams: URLSearchParams | undefined
let summaryParams: URLSearchParams | undefined
let byCategoryParams: URLSearchParams | undefined
let trendParams: URLSearchParams | undefined

beforeEach(() => {
  listParams = summaryParams = byCategoryParams = trendParams = undefined
  server.use(
    http.get('/api/expenses', ({ request }) => {
      // A non-empty page so the list renders the table + Export CSV action
      // (the empty state hides both).
      listParams = new URL(request.url).searchParams
      return HttpResponse.json(sampleExpensePage)
    }),
    http.get('/api/summary', ({ request }) => {
      summaryParams = new URL(request.url).searchParams
      return HttpResponse.json(sampleSummary)
    }),
    http.get('/api/summary/by-category', ({ request }) => {
      byCategoryParams = new URL(request.url).searchParams
      return HttpResponse.json(sampleByCategory)
    }),
    http.get('/api/summary/trend', ({ request }) => {
      trendParams = new URL(request.url).searchParams
      return HttpResponse.json(sampleTrend)
    }),
  )
})

afterEach(() => {
  vi.restoreAllMocks()
})

/** Mounts the filter bar alongside every region that reads the shared filters. */
function renderDashboard() {
  return renderWithProviders(
    <>
      <FilterBar />
      <SummaryRow />
      <TrendSection />
      <ExpenseListSection />
    </>,
    { initialMonth: JUNE_2026 },
  )
}

/** Clicks Export CSV and returns the URL the transient anchor pointed at. */
function exportUrl(): URL {
  const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
  fireEvent.click(screen.getByRole('button', { name: /Export CSV/ }))
  const anchor = clickSpy.mock.instances[0] as HTMLAnchorElement
  const url = new URL(anchor.getAttribute('href')!, 'http://localhost')
  clickSpy.mockRestore()
  return url
}

describe('Filters fan out to list + charts + export (P8-4, phase-8 Exit)', () => {
  it('defaults every region to the selected-month range with no filters', async () => {
    renderDashboard()

    await waitFor(() => expect(listParams).toBeDefined())
    await waitFor(() => expect(summaryParams).toBeDefined())
    await waitFor(() => expect(byCategoryParams).toBeDefined())
    await waitFor(() => expect(trendParams).toBeDefined())

    for (const params of [listParams, summaryParams, byCategoryParams, trendParams]) {
      expect(params?.get('from')).toBe('2026-06-01')
      expect(params?.get('to')).toBe('2026-06-30')
    }
  })

  it('changing the date range refines the list, all three charts, and the export URL', async () => {
    renderDashboard()
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Export CSV/ })).toBeInTheDocument(),
    )

    fireEvent.change(screen.getByLabelText('From date'), { target: { value: '2026-06-10' } })
    fireEvent.change(screen.getByLabelText('To date'), { target: { value: '2026-06-20' } })

    // List + every chart endpoint pick up the overridden range.
    await waitFor(() => expect(listParams?.get('from')).toBe('2026-06-10'))
    await waitFor(() => expect(summaryParams?.get('from')).toBe('2026-06-10'))
    await waitFor(() => expect(byCategoryParams?.get('from')).toBe('2026-06-10'))
    await waitFor(() => expect(trendParams?.get('from')).toBe('2026-06-10'))
    for (const params of [listParams, summaryParams, byCategoryParams, trendParams]) {
      expect(params?.get('to')).toBe('2026-06-20')
    }

    // CSV download reflects the same refined range.
    const url = exportUrl()
    expect(url.pathname).toBe('/api/expenses/export')
    expect(url.searchParams.get('from')).toBe('2026-06-10')
    expect(url.searchParams.get('to')).toBe('2026-06-20')
  })

  it('changing the category refines the list and export but NOT the charts (list-only filter)', async () => {
    renderDashboard()
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Export CSV/ })).toBeInTheDocument(),
    )

    fireEvent.change(screen.getByLabelText('Filter by category'), {
      target: { value: 'GROCERIES' },
    })

    await waitFor(() => expect(listParams?.get('category')).toBe('GROCERIES'))
    expect(exportUrl().searchParams.get('category')).toBe('GROCERIES')

    // Per the API contract the summary endpoints only take a date range, so the
    // category must not leak into the charts.
    expect(summaryParams?.has('category')).toBe(false)
    expect(byCategoryParams?.has('category')).toBe(false)
    expect(trendParams?.has('category')).toBe(false)
  })

  it('changing the amount range refines the list + export, leaving the charts on the date range', async () => {
    renderDashboard()
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Export CSV/ })).toBeInTheDocument(),
    )

    fireEvent.change(screen.getByLabelText('Minimum amount'), { target: { value: '50' } })
    fireEvent.change(screen.getByLabelText('Maximum amount'), { target: { value: '900' } })

    await waitFor(() => expect(listParams?.get('minAmount')).toBe('50'))
    await waitFor(() => expect(listParams?.get('maxAmount')).toBe('900'))

    const url = exportUrl()
    expect(url.searchParams.get('minAmount')).toBe('50')
    expect(url.searchParams.get('maxAmount')).toBe('900')

    expect(summaryParams?.has('minAmount')).toBe(false)
    expect(trendParams?.has('maxAmount')).toBe(false)
  })

  it('typing in search refines the list + export, not the charts', async () => {
    renderDashboard()
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Export CSV/ })).toBeInTheDocument(),
    )

    fireEvent.change(screen.getByLabelText('Search expenses'), { target: { value: 'lunch' } })

    await waitFor(() => expect(listParams?.get('q')).toBe('lunch'))
    expect(exportUrl().searchParams.get('q')).toBe('lunch')
    expect(summaryParams?.has('q')).toBe(false)
    expect(byCategoryParams?.has('q')).toBe(false)
    expect(trendParams?.has('q')).toBe(false)
  })

  it('Clear resets every region back to the selected-month range and drops the filters', async () => {
    renderDashboard()
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Export CSV/ })).toBeInTheDocument(),
    )

    // Apply a date override + a list-only filter, then clear.
    fireEvent.change(screen.getByLabelText('From date'), { target: { value: '2026-06-10' } })
    fireEvent.change(screen.getByLabelText('Search expenses'), { target: { value: 'lunch' } })
    await waitFor(() => expect(listParams?.get('from')).toBe('2026-06-10'))
    await waitFor(() => expect(listParams?.get('q')).toBe('lunch'))

    fireEvent.click(screen.getByRole('button', { name: 'Clear' }))

    await waitFor(() => expect(listParams?.get('from')).toBe('2026-06-01'))
    expect(listParams?.has('q')).toBe(false)
    // Charts are back on the full-month range too.
    await waitFor(() => expect(summaryParams?.get('from')).toBe('2026-06-01'))
    expect(trendParams?.get('to')).toBe('2026-06-30')
    // And the export URL carries only the month range again.
    expect(Object.fromEntries(exportUrl().searchParams)).toEqual({
      from: '2026-06-01',
      to: '2026-06-30',
    })
  })
})
