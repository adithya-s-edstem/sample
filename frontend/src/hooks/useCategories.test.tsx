import { describe, it, expect } from 'vitest'
import { http, HttpResponse } from 'msw'
import { waitFor } from '@testing-library/react'
import { useCategories } from './useCategories'
import { server } from '../test/msw/server'
import { sampleCategories } from '../test/msw/handlers'
import { apiError, renderHookWithClient } from '../test/utils'

describe('useCategories', () => {
  it('returns the fixed category enum set', async () => {
    const { result } = renderHookWithClient(() => useCategories())
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(sampleCategories)
  })

  it('surfaces an error from /categories', async () => {
    server.use(
      http.get('/api/categories', () =>
        HttpResponse.json(apiError({ status: 500 }), { status: 500 }),
      ),
    )
    const { result } = renderHookWithClient(() => useCategories())
    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})
