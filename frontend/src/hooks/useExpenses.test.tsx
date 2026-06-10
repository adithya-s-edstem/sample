import { describe, it, expect } from 'vitest'
import { http, HttpResponse, delay } from 'msw'
import { waitFor } from '@testing-library/react'
import { useExpenses, useExpense } from './useExpenses'
import { server } from '../test/msw/server'
import { sampleExpense, sampleExpensePage } from '../test/msw/handlers'
import { apiError, renderHookWithClient } from '../test/utils'

describe('useExpenses', () => {
  it('starts in a loading state then resolves with the page', async () => {
    server.use(
      http.get('/api/expenses', async () => {
        await delay(20)
        return HttpResponse.json(sampleExpensePage)
      }),
    )

    const { result } = renderHookWithClient(() => useExpenses())

    expect(result.current.isLoading).toBe(true)
    expect(result.current.data).toBeUndefined()

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(sampleExpensePage)
    // Money keeps its exact two-decimal value through the wire.
    expect(result.current.data?.content[0].amount).toBe(1234.56)
  })

  it('surfaces an error when the request fails', async () => {
    server.use(
      http.get('/api/expenses', () =>
        HttpResponse.json(apiError({ status: 500, message: 'boom' }), { status: 500 }),
      ),
    )

    const { result } = renderHookWithClient(() => useExpenses())

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(result.current.data).toBeUndefined()
  })

  it('keys the cache by the query so distinct filters fetch separately', async () => {
    let calls = 0
    server.use(
      http.get('/api/expenses', () => {
        calls += 1
        return HttpResponse.json(sampleExpensePage)
      }),
    )

    const { result, rerender, queryClient } = renderHookWithClient(
      ({ category }: { category: 'FOOD' | 'RENT' }) => useExpenses({ category }),
      { initialProps: { category: 'FOOD' } as { category: 'FOOD' | 'RENT' } },
    )
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(calls).toBe(1)

    rerender({ category: 'RENT' })
    await waitFor(() => expect(calls).toBe(2))

    // Two distinct cache entries, one per filter set.
    expect(queryClient.getQueryData(['expenses', 'list', { category: 'FOOD' }])).toBeDefined()
    expect(queryClient.getQueryData(['expenses', 'list', { category: 'RENT' }])).toBeDefined()
  })
})

describe('useExpense', () => {
  it('is disabled (no fetch) until an id is provided', async () => {
    let calls = 0
    server.use(
      http.get('/api/expenses/:id', () => {
        calls += 1
        return HttpResponse.json(sampleExpense)
      }),
    )

    const { result } = renderHookWithClient(() => useExpense(undefined))

    // A disabled query is pending but not actively loading (fetchStatus idle).
    expect(result.current.fetchStatus).toBe('idle')
    expect(result.current.isPending).toBe(true)
    expect(result.current.isLoading).toBe(false)
    // Give any erroneous fetch a chance to fire.
    await new Promise((r) => setTimeout(r, 30))
    expect(calls).toBe(0)
  })

  it('fetches the detail once an id is set', async () => {
    const { result } = renderHookWithClient(() => useExpense(sampleExpense.id))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data?.id).toBe(sampleExpense.id)
  })
})
