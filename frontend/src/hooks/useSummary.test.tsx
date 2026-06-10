import { describe, it, expect } from 'vitest'
import { http, HttpResponse, delay } from 'msw'
import { waitFor } from '@testing-library/react'
import { useSummary, useSummaryByCategory, useSummaryTrend } from './useSummary'
import { server } from '../test/msw/server'
import { sampleByCategory, sampleSummary, sampleTrend } from '../test/msw/handlers'
import { apiError, renderHookWithClient } from '../test/utils'

describe('useSummary', () => {
  it('loads then returns the headline total + count', async () => {
    server.use(
      http.get('/api/summary', async () => {
        await delay(20)
        return HttpResponse.json(sampleSummary)
      }),
    )

    const { result } = renderHookWithClient(() => useSummary())
    expect(result.current.isLoading).toBe(true)

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(sampleSummary)
    expect(result.current.data?.total).toBe(1234.56)
  })

  it('surfaces an error from /summary', async () => {
    server.use(
      http.get('/api/summary', () => HttpResponse.json(apiError({ status: 500 }), { status: 500 })),
    )
    const { result } = renderHookWithClient(() => useSummary())
    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})

describe('useSummaryByCategory', () => {
  it('returns the per-category breakdown for the donut', async () => {
    const { result } = renderHookWithClient(() => useSummaryByCategory())
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(sampleByCategory)
    expect(result.current.data?.categories[0].percent).toBe(100)
  })

  it('surfaces an error from /summary/by-category', async () => {
    server.use(
      http.get('/api/summary/by-category', () =>
        HttpResponse.json(apiError({ status: 500 }), { status: 500 }),
      ),
    )
    const { result } = renderHookWithClient(() => useSummaryByCategory())
    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})

describe('useSummaryTrend', () => {
  it('returns the time series for the trend chart', async () => {
    const { result } = renderHookWithClient(() => useSummaryTrend())
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(sampleTrend)
  })

  it('surfaces an error from /summary/trend', async () => {
    server.use(
      http.get('/api/summary/trend', () =>
        HttpResponse.json(apiError({ status: 500 }), { status: 500 }),
      ),
    )
    const { result } = renderHookWithClient(() => useSummaryTrend())
    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})
