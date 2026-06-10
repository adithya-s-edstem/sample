import { describe, it, expect, vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { screen, fireEvent, waitFor } from '@testing-library/react'
import DeleteExpenseDialog from './DeleteExpenseDialog'
import { queryKeys } from '../../api/queryKeys'
import { server } from '../../test/msw/server'
import { apiError, renderWithProviders } from '../../test/utils'
import type { Expense } from '../../api/types'

/*
 * DeleteExpenseDialog container tests (P7-4): the confirm prompt is wired to the
 * API. Confirming issues DELETE /api/expenses/{id}, invalidates the list + every
 * summary + that detail so the dashboard refreshes, and closes on success; a
 * failed delete is surfaced inline without closing. Per docs/solution.md §4 and
 * docs/testing-plan.md §4 (cache-invalidation-on-mutation) / §6 (delete → row
 * removed, totals update).
 */

const expense: Expense = {
  id: '11111111-1111-1111-1111-111111111111',
  amount: 1234.5,
  date: '2026-06-05',
  category: 'GROCERIES',
  createdAt: '2026-06-05T09:30:00Z',
  updatedAt: '2026-06-05T09:30:00Z',
}

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

describe('DeleteExpenseDialog (P7-4)', () => {
  it('echoes the expense details in the confirm copy', () => {
    renderWithProviders(<DeleteExpenseDialog expense={expense} onClose={vi.fn()} />)
    const dialog = screen.getByRole('alertdialog')
    expect(dialog).toHaveTextContent('₹1,234.50')
    expect(dialog).toHaveTextContent('Groceries')
    expect(dialog).toHaveTextContent('5 Jun 2026')
  })

  it('deletes the expense and closes, invalidating list, summaries, and that detail', async () => {
    let deletedId: string | undefined
    server.use(
      http.delete('/api/expenses/:id', ({ params }) => {
        deletedId = params.id as string
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const onClose = vi.fn()
    const { queryClient } = renderWithProviders(
      <DeleteExpenseDialog expense={expense} onClose={onClose} />,
    )
    seedCaches(queryClient)

    fireEvent.click(screen.getByRole('button', { name: 'Delete' }))

    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1))
    expect(deletedId).toBe(expense.id)
    expect(isInvalidated(queryClient, queryKeys.expenses.list({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.summary.totals({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.summary.byCategory({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.summary.trend({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.expenses.detail(expense.id))).toBe(true)
  })

  it('does not delete when Cancel is clicked', () => {
    const deleted = vi.fn()
    server.use(
      http.delete('/api/expenses/:id', () => {
        deleted()
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const onClose = vi.fn()
    renderWithProviders(<DeleteExpenseDialog expense={expense} onClose={onClose} />)

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(onClose).toHaveBeenCalledTimes(1)
    expect(deleted).not.toHaveBeenCalled()
  })

  it('surfaces a server error inline and stays open on a failed delete', async () => {
    server.use(
      http.delete('/api/expenses/:id', () =>
        HttpResponse.json(apiError({ status: 404, message: 'Expense not found' }), { status: 404 }),
      ),
    )
    const onClose = vi.fn()
    renderWithProviders(<DeleteExpenseDialog expense={expense} onClose={onClose} />)

    fireEvent.click(screen.getByRole('button', { name: 'Delete' }))

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Expense not found'))
    expect(onClose).not.toHaveBeenCalled()
  })
})
