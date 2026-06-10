import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { screen, waitFor, fireEvent, within } from '@testing-library/react'
import App from './App'
import { server } from './test/msw/server'
import { renderWithProviders } from './test/utils'
import type { Category, Expense } from './api/types'

/*
 * App-level integration tests (P7-5) for the Phase 7 exit criterion:
 * "add/edit/delete work end to end and the dashboard updates live".
 *
 * Rather than asserting cache invalidation directly (covered in the
 * ExpenseFormModal / DeleteExpenseDialog container tests), these drive the real
 * App through the user-facing flow against a stateful MSW backend: a mutating
 * write (POST/PUT/DELETE) changes the in-memory store, and because the mutations
 * invalidate the list + summary roots, the dashboard re-fetches and reflects the
 * change — the row appears/updates/disappears and the "This Month" total moves.
 *
 * The store is seeded in June 2026 (the pinned selected month) so the resolved
 * current-month range and assertions are deterministic. Summary/by-category/trend
 * are derived from the store so the total card tracks the list live.
 */

type Row = Pick<Expense, 'id' | 'amount' | 'date' | 'category'>

let store: Row[]
let nextId = 0

function full(row: Row): Expense {
  return { ...row, createdAt: '2026-06-01T00:00:00Z', updatedAt: '2026-06-01T00:00:00Z' }
}

function total(): number {
  return Number(store.reduce((sum, r) => sum + r.amount, 0).toFixed(2))
}

beforeEach(() => {
  nextId = 0
  store = [{ id: 'seed-1', amount: 1200, date: '2026-06-05', category: 'GROCERIES' }]

  server.use(
    http.get('/api/expenses', () =>
      HttpResponse.json({
        content: store.map(full),
        page: 0,
        size: 50,
        totalElements: store.length,
        totalPages: store.length === 0 ? 0 : 1,
        sort: 'date,desc',
      }),
    ),
    http.post('/api/expenses', async ({ request }) => {
      const body = (await request.json()) as { amount: number; date: string; category: Category }
      const row: Row = { id: `new-${nextId++}`, ...body }
      store = [row, ...store]
      return HttpResponse.json(full(row), { status: 201 })
    }),
    http.put('/api/expenses/:id', async ({ params, request }) => {
      const body = (await request.json()) as { amount: number; date: string; category: Category }
      const id = params.id as string
      store = store.map((r) => (r.id === id ? { ...r, ...body } : r))
      return HttpResponse.json(full({ id, ...body }))
    }),
    http.delete('/api/expenses/:id', ({ params }) => {
      store = store.filter((r) => r.id !== (params.id as string))
      return new HttpResponse(null, { status: 204 })
    }),
    http.get('/api/summary', () =>
      HttpResponse.json({
        from: '2026-06-01',
        to: '2026-06-30',
        total: total(),
        count: store.length,
        currency: 'INR',
      }),
    ),
    http.get('/api/summary/by-category', () =>
      HttpResponse.json({ from: '2026-06-01', to: '2026-06-30', total: total(), categories: [] }),
    ),
    http.get('/api/summary/trend', () =>
      HttpResponse.json({ from: '2026-06-01', to: '2026-06-30', granularity: 'day', points: [] }),
    ),
  )
})

const JUNE_2026 = new Date(2026, 5, 1)

function renderApp() {
  return renderWithProviders(<App />, { initialMonth: JUNE_2026 })
}

describe('App — Phase 7 exit: add/edit/delete update the dashboard live', () => {
  it('adds an expense and the new row + updated total appear without a manual reload', async () => {
    renderApp()

    // Seeded row + its total are shown.
    await waitFor(() => expect(screen.getByText('5 Jun 2026')).toBeInTheDocument())
    expect(screen.getByText('₹1,200.00')).toBeInTheDocument()

    // Open the add modal from the header and fill a valid expense.
    fireEvent.click(screen.getByRole('button', { name: /Add Expense/ }))
    expect(screen.getByRole('heading', { name: 'Add Expense' })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '300.50' } })
    fireEvent.change(screen.getByLabelText('Date'), { target: { value: '2026-06-09' } })
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'TRANSPORT' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save Expense' }))

    // Modal closes and the dashboard reflects the new row + new total live.
    await waitFor(() =>
      expect(screen.queryByRole('heading', { name: 'Add Expense' })).not.toBeInTheDocument(),
    )
    await waitFor(() => expect(screen.getByText('9 Jun 2026')).toBeInTheDocument())
    // Scope category text to the table — the filter bar's category dropdown also
    // renders a "Transport" <option> (P8-1).
    const addedTable = screen.getByRole('table')
    expect(within(addedTable).getByText('Transport')).toBeInTheDocument()
    expect(screen.getByText('₹300.50')).toBeInTheDocument()
    // 1200 + 300.50 = 1500.50 in the "This Month" total.
    await waitFor(() => expect(screen.getByText('₹1,500.50')).toBeInTheDocument())
  })

  it('edits an expense: the modal pre-fills and the saved row + total update live', async () => {
    renderApp()
    await waitFor(() => expect(screen.getByText('5 Jun 2026')).toBeInTheDocument())

    const table = screen.getByRole('table')
    fireEvent.click(within(table).getByRole('button', { name: /^Edit expense/ }))

    // Edit modal opens pre-filled from the row.
    expect(screen.getByRole('heading', { name: 'Edit Expense' })).toBeInTheDocument()
    expect(screen.getByLabelText('Amount (₹)')).toHaveValue('1200')
    expect(screen.getByLabelText('Date')).toHaveValue('2026-06-05')
    expect(screen.getByLabelText('Category')).toHaveValue('GROCERIES')

    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '1750' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save Expense' }))

    await waitFor(() =>
      expect(screen.queryByRole('heading', { name: 'Edit Expense' })).not.toBeInTheDocument(),
    )
    // The row's amount and the month total both move to the new value live.
    await waitFor(() => expect(screen.getByText('₹1,750.00')).toBeInTheDocument())
    expect(screen.queryByText('₹1,200.00')).not.toBeInTheDocument()
  })

  it('exposes a skip-to-content link and a main landmark for keyboard/AT users (P9-1)', async () => {
    renderApp()
    await waitFor(() => expect(screen.getByText('5 Jun 2026')).toBeInTheDocument())

    // Skip link targets the main content region so keyboard users can bypass the header.
    const skip = screen.getByRole('link', { name: 'Skip to content' })
    expect(skip).toHaveAttribute('href', '#main-content')
    const main = screen.getByRole('main')
    expect(main).toHaveAttribute('id', 'main-content')
  })

  it('deletes an expense after confirm: the row disappears and the empty state shows live', async () => {
    renderApp()
    await waitFor(() => expect(screen.getByText('5 Jun 2026')).toBeInTheDocument())

    const table = screen.getByRole('table')
    fireEvent.click(within(table).getByRole('button', { name: /^Delete expense/ }))

    // Confirm prompt before the destructive delete.
    expect(screen.getByRole('heading', { name: 'Delete expense?' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }))

    // After the delete the month has no rows → the live dashboard flips to empty.
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'No expenses this month' })).toBeInTheDocument(),
    )
    expect(screen.queryByText('5 Jun 2026')).not.toBeInTheDocument()
    // The "This Month" total uses whole-rupee formatting, so an empty month reads ₹0.
    await waitFor(() => expect(screen.getByText('₹0')).toBeInTheDocument())
  })
})
