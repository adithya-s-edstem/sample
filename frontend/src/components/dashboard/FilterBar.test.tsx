import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { screen, fireEvent, waitFor } from '@testing-library/react'
import FilterBar from './FilterBar'
import ExpenseListSection from './ExpenseListSection'
import { server } from '../../test/msw/server'
import { renderWithProviders } from '../../test/utils'
import type { PageResponse, Expense } from '../../api/types'

/*
 * FilterBar tests (P8-1): the controlled inputs are bound to the filter state,
 * and changing a filter folds into the `GET /api/expenses` query params. We
 * mount the FilterBar alongside the ExpenseListSection (both read the shared
 * FilterProvider) and capture each outgoing request's querystring to assert the
 * params actually change.
 */

const JUNE_2026 = new Date(2026, 5, 1)

const emptyPage: PageResponse<Expense> = {
  content: [],
  page: 0,
  size: 50,
  totalElements: 0,
  totalPages: 0,
  sort: 'date,desc',
}

let requests: URL[]

beforeEach(() => {
  requests = []
  server.use(
    http.get('/api/expenses', ({ request }) => {
      requests.push(new URL(request.url))
      return HttpResponse.json(emptyPage)
    }),
  )
})

/** The querystring of the most recent /api/expenses request. */
function lastParams(): URLSearchParams {
  return requests[requests.length - 1].searchParams
}

function renderBar(initialFilters = {}) {
  return renderWithProviders(
    <>
      <FilterBar />
      <ExpenseListSection />
    </>,
    { initialMonth: JUNE_2026, initialFilters },
  )
}

describe('FilterBar — bound to the list query params (P8-1)', () => {
  it('defaults the list query to the selected month with no filters', async () => {
    renderBar()
    await waitFor(() => expect(requests.length).toBeGreaterThan(0))
    expect(lastParams().get('from')).toBe('2026-06-01')
    expect(lastParams().get('to')).toBe('2026-06-30')
    expect(lastParams().has('category')).toBe(false)
  })

  it('renders controlled inputs seeded from the filter state', () => {
    renderBar({ category: 'TRANSPORT', minAmount: '100', maxAmount: '500', q: 'coffee' })
    expect(screen.getByLabelText('Filter by category')).toHaveValue('TRANSPORT')
    expect(screen.getByLabelText('Minimum amount')).toHaveValue(100)
    expect(screen.getByLabelText('Maximum amount')).toHaveValue(500)
    expect(screen.getByLabelText('Search expenses')).toHaveValue('coffee')
  })

  it('changing the category drives the list query category param', async () => {
    renderBar()
    fireEvent.change(screen.getByLabelText('Filter by category'), {
      target: { value: 'GROCERIES' },
    })
    await waitFor(() => expect(lastParams().get('category')).toBe('GROCERIES'))
  })

  it('changing the amount range drives minAmount / maxAmount', async () => {
    renderBar()
    fireEvent.change(screen.getByLabelText('Minimum amount'), { target: { value: '50' } })
    fireEvent.change(screen.getByLabelText('Maximum amount'), { target: { value: '900' } })
    await waitFor(() => expect(lastParams().get('minAmount')).toBe('50'))
    await waitFor(() => expect(lastParams().get('maxAmount')).toBe('900'))
  })

  it('typing in search drives the q param', async () => {
    renderBar()
    fireEvent.change(screen.getByLabelText('Search expenses'), { target: { value: 'lunch' } })
    await waitFor(() => expect(lastParams().get('q')).toBe('lunch'))
  })

  it('the date-range inputs override the month range', async () => {
    renderBar()
    fireEvent.change(screen.getByLabelText('From date'), { target: { value: '2026-06-10' } })
    fireEvent.change(screen.getByLabelText('To date'), { target: { value: '2026-06-20' } })
    await waitFor(() => expect(lastParams().get('from')).toBe('2026-06-10'))
    await waitFor(() => expect(lastParams().get('to')).toBe('2026-06-20'))
  })

  it('shows a Clear control only when a filter is active and resets the query', async () => {
    renderBar()
    expect(screen.queryByRole('button', { name: 'Clear' })).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Search expenses'), { target: { value: 'lunch' } })
    await waitFor(() => expect(lastParams().get('q')).toBe('lunch'))

    const clear = screen.getByRole('button', { name: 'Clear' })
    fireEvent.click(clear)

    await waitFor(() => expect(lastParams().has('q')).toBe(false))
    expect(screen.getByLabelText('Search expenses')).toHaveValue('')
    expect(screen.queryByRole('button', { name: 'Clear' })).not.toBeInTheDocument()
  })
})
