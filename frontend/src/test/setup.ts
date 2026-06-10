import '@testing-library/jest-dom/vitest'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { cleanup } from '@testing-library/react'
import { server } from './msw/server'

/*
 * Global Vitest setup. Starts the MSW request-interception server once for the
 * whole suite, resets per-test handler overrides between tests, and unmounts any
 * rendered React trees so cases stay isolated. `onUnhandledRequest: 'error'`
 * surfaces any request a test forgot to mock instead of silently hitting the
 * (non-existent) real backend.
 */
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

afterEach(() => {
  cleanup()
  server.resetHandlers()
})

afterAll(() => server.close())
