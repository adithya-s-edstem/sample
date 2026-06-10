import { describe, it, expect } from 'vitest'
import { http, HttpResponse } from 'msw'
import { waitFor } from '@testing-library/react'
import { useCreateExpense, useUpdateExpense, useDeleteExpense } from './useExpenseMutations'
import { queryKeys } from '../api/queryKeys'
import { server } from '../test/msw/server'
import { sampleExpense } from '../test/msw/handlers'
import { apiError, renderHookWithClient } from '../test/utils'
import type { ExpenseRequest } from '../api/types'

const body: ExpenseRequest = { amount: 50.25, date: '2026-06-10', category: 'FOOD' }

/* Seeds list + summary + detail caches so we can observe invalidation. */
function seedCaches(queryClient: ReturnType<typeof renderHookWithClient>['queryClient']) {
  queryClient.setQueryData(queryKeys.expenses.list({}), { stale: false })
  queryClient.setQueryData(queryKeys.summary.totals({}), { stale: false })
  queryClient.setQueryData(queryKeys.summary.byCategory({}), { stale: false })
  queryClient.setQueryData(queryKeys.summary.trend({}), { stale: false })
  queryClient.setQueryData(queryKeys.expenses.detail(sampleExpense.id), { stale: false })
}

function isInvalidated(
  queryClient: ReturnType<typeof renderHookWithClient>['queryClient'],
  key: readonly unknown[],
): boolean {
  return queryClient.getQueryState(key)?.isInvalidated === true
}

describe('useCreateExpense', () => {
  it('creates then invalidates the list and every summary', async () => {
    const { result, queryClient } = renderHookWithClient(() => useCreateExpense())
    seedCaches(queryClient)

    result.current.mutate(body)
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(result.current.data?.amount).toBe(50.25)
    expect(isInvalidated(queryClient, queryKeys.expenses.list({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.summary.totals({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.summary.byCategory({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.summary.trend({}))).toBe(true)
  })

  it('does not invalidate caches when the create fails', async () => {
    server.use(
      http.post('/api/expenses', () =>
        HttpResponse.json(apiError({ status: 400, message: 'amount must be > 0' }), {
          status: 400,
        }),
      ),
    )
    const { result, queryClient } = renderHookWithClient(() => useCreateExpense())
    seedCaches(queryClient)

    result.current.mutate(body)
    await waitFor(() => expect(result.current.isError).toBe(true))

    expect(isInvalidated(queryClient, queryKeys.expenses.list({}))).toBe(false)
    expect(isInvalidated(queryClient, queryKeys.summary.totals({}))).toBe(false)
  })
})

describe('useUpdateExpense', () => {
  it('updates then invalidates list, summaries, and that detail', async () => {
    const { result, queryClient } = renderHookWithClient(() => useUpdateExpense())
    seedCaches(queryClient)

    result.current.mutate({ id: sampleExpense.id, body })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(isInvalidated(queryClient, queryKeys.expenses.list({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.summary.totals({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.expenses.detail(sampleExpense.id))).toBe(true)
  })
})

describe('useDeleteExpense', () => {
  it('deletes then invalidates list, summaries, and that detail', async () => {
    const { result, queryClient } = renderHookWithClient(() => useDeleteExpense())
    seedCaches(queryClient)

    result.current.mutate(sampleExpense.id)
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(isInvalidated(queryClient, queryKeys.expenses.list({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.summary.byCategory({}))).toBe(true)
    expect(isInvalidated(queryClient, queryKeys.expenses.detail(sampleExpense.id))).toBe(true)
  })

  it('does not invalidate caches when the delete fails', async () => {
    server.use(
      http.delete('/api/expenses/:id', () =>
        HttpResponse.json(apiError({ status: 404, message: 'not found' }), { status: 404 }),
      ),
    )
    const { result, queryClient } = renderHookWithClient(() => useDeleteExpense())
    seedCaches(queryClient)

    result.current.mutate(sampleExpense.id)
    await waitFor(() => expect(result.current.isError).toBe(true))

    expect(isInvalidated(queryClient, queryKeys.expenses.list({}))).toBe(false)
    expect(isInvalidated(queryClient, queryKeys.expenses.detail(sampleExpense.id))).toBe(false)
  })
})
