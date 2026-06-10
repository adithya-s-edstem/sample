import { describe, it, expect, vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { screen, fireEvent, waitFor } from '@testing-library/react'
import ExpenseFormModal from './ExpenseFormModal'
import { queryKeys } from '../../api/queryKeys'
import { server } from '../../test/msw/server'
import { apiError, renderWithProviders } from '../../test/utils'
import type { Expense } from '../../api/types'

/*
 * ExpenseFormModal container tests (P7-3): the modal is wired to the API. A valid
 * Save issues a create (add mode) or update (edit mode), the dashboard caches are
 * invalidated so the list/summaries refresh, the modal closes on success, and a
 * server-side failure is surfaced inline without closing. Per docs/testing-plan.md
 * §4 (cache-invalidation-on-mutation) and §6 (exact two-decimal money on the wire).
 */

const expense: Expense = {
  id: '11111111-1111-1111-1111-111111111111',
  amount: 1234.5,
  date: '2026-06-05',
  category: 'GROCERIES',
  createdAt: '2026-06-05T09:30:00Z',
  updatedAt: '2026-06-05T09:30:00Z',
}

/* Seeds the list + every summary cache so we can observe invalidation. */
function seedCaches(queryClient: ReturnType<typeof renderWithProviders>['queryClient']) {
  queryClient.setQueryData(queryKeys.expenses.list({}), { stale: false })
  queryClient.setQueryData(queryKeys.summary.totals({}), { stale: false })
  queryClient.setQueryData(queryKeys.summary.byCategory({}), { stale: false })
  queryClient.setQueryData(queryKeys.summary.trend({}), { stale: false })
  queryClient.setQueryData(queryKeys.expenses.detail(expense.id), { stale: false })
}

function isInvalidated(
  queryClient: ReturnType<typeof renderWithProviders>['queryClient'],
  key: readonly unknown[],
): boolean {
  return queryClient.getQueryState(key)?.isInvalidated === true
}

describe('ExpenseFormModal — add mode (P7-3)', () => {
  it('creates an expense and closes, invalidating the list and every summary', async () => {
    const created = vi.fn()
    server.use(
      http.post('/api/expenses', async ({ request }) => {
        created()
        const body = await request.json()
        return HttpResponse.json({ ...expense, ...(body as object) }, { status: 201 })
      }),
    )
    const onClose = vi.fn()
    const { queryClient } = renderWithProviders(<ExpenseFormModal onClose={onClose} />)
    seedCaches(queryClient)

    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '1200.50' } })
    fireEvent.change(screen.getByLabelText('Date'), { target: { value: '2026-06-08' } })
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'RENT' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save Expense' }))

    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1))
    expect(created).toHaveBeenCalledTimes(1)
    expect(isInvalidated(queryClient, queryKeys.expenses.list({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.summary.totals({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.summary.byCategory({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.summary.trend({}))).toBe(true)
  })

  it('sends the exact two-decimal amount on the wire', async () => {
    let sent: { amount?: number } = {}
    server.use(
      http.post('/api/expenses', async ({ request }) => {
        sent = (await request.json()) as { amount?: number }
        return HttpResponse.json({ ...expense, ...sent }, { status: 201 })
      }),
    )
    const { queryClient } = renderWithProviders(<ExpenseFormModal onClose={vi.fn()} />)
    seedCaches(queryClient)

    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '0.01' } })
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'FOOD' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save Expense' }))

    await waitFor(() => expect(sent.amount).toBe(0.01))
  })

  it('surfaces a server validation error inline and stays open', async () => {
    server.use(
      http.post('/api/expenses', () =>
        HttpResponse.json(apiError({ status: 400, message: 'amount must be greater than 0' }), {
          status: 400,
        }),
      ),
    )
    const onClose = vi.fn()
    renderWithProviders(<ExpenseFormModal onClose={onClose} />)

    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '10' } })
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'FOOD' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save Expense' }))

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('amount must be greater than 0'),
    )
    expect(onClose).not.toHaveBeenCalled()
  })
})

describe('ExpenseFormModal — edit mode (P7-3)', () => {
  it('updates the expense and closes, invalidating list, summaries, and that detail', async () => {
    let putId: string | undefined
    server.use(
      http.put('/api/expenses/:id', async ({ params, request }) => {
        putId = params.id as string
        const body = await request.json()
        return HttpResponse.json({ ...expense, ...(body as object), id: putId })
      }),
    )
    const onClose = vi.fn()
    const { queryClient } = renderWithProviders(
      <ExpenseFormModal expense={expense} onClose={onClose} />,
    )
    seedCaches(queryClient)

    expect(screen.getByRole('heading', { name: 'Edit Expense' })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '1500' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save Expense' }))

    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1))
    expect(putId).toBe(expense.id)
    expect(isInvalidated(queryClient, queryKeys.expenses.list({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.summary.totals({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.expenses.detail(expense.id))).toBe(true)
  })
})
