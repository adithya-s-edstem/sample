import { describe, it, expect } from 'vitest'
import { http, HttpResponse, delay } from 'msw'
import { screen, waitFor } from '@testing-library/react'
import ExpenseListSection from './ExpenseListSection'
import { server } from '../../test/msw/server'
import { sampleExpensePage } from '../../test/msw/handlers'
import { apiError, renderWithProviders } from '../../test/utils'
import type { PageResponse, Expense } from '../../api/types'

// June 2026 is pinned so the resolved range feeding the query is deterministic.
const JUNE_2026 = new Date(2026, 5, 1)

const emptyPage: PageResponse<Expense> = {
  content: [],
  page: 0,
  size: 50,
  totalElements: 0,
  totalPages: 0,
  sort: 'date,desc',
}

describe('ExpenseListSection — empty state (P6-4)', () => {
  it('renders the empty-state prompt when the month has no expenses', async () => {
    server.use(http.get('/api/expenses', () => HttpResponse.json(emptyPage)))

    renderWithProviders(<ExpenseListSection />, { initialMonth: JUNE_2026 })

    await waitFor(() => expect(screen.getByText('No expenses this month')).toBeInTheDocument())
    expect(
      screen.getByText('Start tracking where your money goes by adding your first expense.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Add your first expense' })).toBeInTheDocument()
  })

  it('hides the table header and Export action in the empty state', async () => {
    server.use(http.get('/api/expenses', () => HttpResponse.json(emptyPage)))

    renderWithProviders(<ExpenseListSection />, { initialMonth: JUNE_2026 })

    await waitFor(() => expect(screen.getByText('No expenses this month')).toBeInTheDocument())
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Export CSV/ })).not.toBeInTheDocument()
  })

  it('renders the table (not the empty state) when the month has expenses', async () => {
    server.use(http.get('/api/expenses', () => HttpResponse.json(sampleExpensePage)))

    renderWithProviders(<ExpenseListSection />, { initialMonth: JUNE_2026 })

    await waitFor(() => expect(screen.getByRole('table')).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /Export CSV/ })).toBeInTheDocument()
    expect(screen.queryByText('No expenses this month')).not.toBeInTheDocument()
  })

  it('shows the row skeletons while /api/expenses is pending', () => {
    server.use(
      http.get('/api/expenses', async () => {
        await delay('infinite')
        return HttpResponse.json(emptyPage)
      }),
    )

    renderWithProviders(<ExpenseListSection />, { initialMonth: JUNE_2026 })
    expect(screen.getByRole('table')).toBeInTheDocument()
    expect(screen.queryByText('No expenses this month')).not.toBeInTheDocument()
  })

  it('shows a graceful error + Retry when /api/expenses fails', async () => {
    server.use(
      http.get('/api/expenses', () =>
        HttpResponse.json(apiError({ status: 500 }), { status: 500 }),
      ),
    )

    renderWithProviders(<ExpenseListSection />, { initialMonth: JUNE_2026 })
    await waitFor(() => expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument())
    expect(screen.queryByText('No expenses this month')).not.toBeInTheDocument()
  })
})
