import { type ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, renderHook, type RenderHookOptions } from '@testing-library/react'
import { MonthProvider } from '../context/MonthContext.tsx'
import type { Month } from '../lib/month'
import type { ApiError } from '../api/types'

/*
 * Test helpers for the hook suite. Each test gets its own QueryClient so caches
 * never leak between cases; retries are disabled so error states resolve on the
 * first failed request instead of after TanStack Query's default backoff.
 */

export function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
}

/** Renders a hook inside a fresh QueryClientProvider and returns the client too. */
export function renderHookWithClient<Result, Props>(
  hook: (initialProps: Props) => Result,
  options?: Omit<RenderHookOptions<Props>, 'wrapper'>,
) {
  const queryClient = createTestQueryClient()
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  const view = renderHook(hook, { wrapper, ...options })
  return { ...view, queryClient }
}

/**
 * Renders a component inside a fresh QueryClientProvider + MonthProvider, the
 * two contexts every dashboard section depends on. `initialMonth` pins the
 * selected month so date-dependent assertions (e.g. the "June 2026" subtitle)
 * are deterministic regardless of when the suite runs.
 */
export function renderWithProviders(
  ui: ReactNode,
  { initialMonth }: { initialMonth?: Month } = {},
) {
  const queryClient = createTestQueryClient()
  const wrapper = (
    <QueryClientProvider client={queryClient}>
      <MonthProvider initialMonth={initialMonth}>{ui}</MonthProvider>
    </QueryClientProvider>
  )
  return { ...render(wrapper), queryClient }
}

/** Builds the backend's uniform error body for MSW error-case handlers. */
export function apiError(partial: Partial<ApiError> & Pick<ApiError, 'status'>): ApiError {
  return {
    timestamp: '2026-06-10T00:00:00Z',
    error: 'Error',
    message: 'Something went wrong',
    path: '/api/expenses',
    ...partial,
  }
}
