import { describe, it, expect, vi } from 'vitest'
import { http, HttpResponse, delay } from 'msw'
import { screen, waitFor, fireEvent } from '@testing-library/react'
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

  it('renders a row per expense in the page content (P7-1)', async () => {
    const page: PageResponse<Expense> = {
      ...sampleExpensePage,
      content: [
        {
          id: 'a',
          amount: 1200,
          date: '2026-06-10',
          category: 'GROCERIES',
          createdAt: '2026-06-10T09:30:00Z',
          updatedAt: '2026-06-10T09:30:00Z',
        },
        {
          id: 'b',
          amount: 300,
          date: '2026-06-09',
          category: 'TRANSPORT',
          createdAt: '2026-06-09T09:30:00Z',
          updatedAt: '2026-06-09T09:30:00Z',
        },
      ],
      totalElements: 2,
      totalPages: 1,
    }
    server.use(http.get('/api/expenses', () => HttpResponse.json(page)))

    renderWithProviders(<ExpenseListSection />, { initialMonth: JUNE_2026 })

    await waitFor(() => expect(screen.getByText('10 Jun 2026')).toBeInTheDocument())
    expect(screen.getByText('Groceries')).toBeInTheDocument()
    expect(screen.getByText('₹1,200.00')).toBeInTheDocument()
    expect(screen.getByText('9 Jun 2026')).toBeInTheDocument()
    expect(screen.getByText('Transport')).toBeInTheDocument()
    expect(screen.getByText('₹300.00')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /^Edit expense/ })).toHaveLength(2)
    expect(screen.getAllByRole('button', { name: /^Delete expense/ })).toHaveLength(2)
  })

  it("fires onEditExpense with the row's expense when its edit action is clicked (P7-3)", async () => {
    server.use(http.get('/api/expenses', () => HttpResponse.json(sampleExpensePage)))
    const onEditExpense = vi.fn()

    renderWithProviders(<ExpenseListSection onEditExpense={onEditExpense} />, {
      initialMonth: JUNE_2026,
    })

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /^Edit expense/ })).toBeInTheDocument(),
    )
    fireEvent.click(screen.getByRole('button', { name: /^Edit expense/ }))
    expect(onEditExpense).toHaveBeenCalledWith(sampleExpensePage.content[0])
  })

  it("fires onDeleteExpense with the row's expense when its delete action is clicked (P7-4)", async () => {
    server.use(http.get('/api/expenses', () => HttpResponse.json(sampleExpensePage)))
    const onDeleteExpense = vi.fn()

    renderWithProviders(<ExpenseListSection onDeleteExpense={onDeleteExpense} />, {
      initialMonth: JUNE_2026,
    })

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /^Delete expense/ })).toBeInTheDocument(),
    )
    fireEvent.click(screen.getByRole('button', { name: /^Delete expense/ }))
    expect(onDeleteExpense).toHaveBeenCalledWith(sampleExpensePage.content[0])
  })

  it('passes onAddExpense to the empty-state CTA (P7-3)', async () => {
    server.use(http.get('/api/expenses', () => HttpResponse.json(emptyPage)))
    const onAddExpense = vi.fn()

    renderWithProviders(<ExpenseListSection onAddExpense={onAddExpense} />, {
      initialMonth: JUNE_2026,
    })

    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Add your first expense' })).toBeInTheDocument(),
    )
    fireEvent.click(screen.getByRole('button', { name: 'Add your first expense' }))
    expect(onAddExpense).toHaveBeenCalledTimes(1)
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
