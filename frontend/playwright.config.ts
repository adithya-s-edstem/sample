import { defineConfig, devices } from '@playwright/test'

/*
 * Playwright E2E config (P9-4) for the key user flows in docs/testing-plan.md §5.
 *
 * The suite drives the real built SPA in a browser. Per the testing plan the
 * flows exercise the app against a backend seeded with known data; here that
 * backend is a deterministic, stateful in-memory fake mounted via Playwright
 * route interception (see e2e/fixtures.ts), so the suite is hermetic and runs in
 * CI without standing up Docker + Postgres + the JVM. It still exercises the real
 * React app, routing, TanStack Query caching/invalidation, and the actual user
 * interactions end to end — the API boundary is the only thing faked, and it is
 * faked to match docs/api-contracts.md exactly.
 *
 * To instead run against the real stack (docker compose up + spring-boot:run +
 * the live API), set E2E_BASE_URL to the running app and the fixture's route
 * mounting can be skipped — left as a documented option, not the default CI path.
 */
const PORT = 4173
const baseURL = process.env.E2E_BASE_URL ?? `http://localhost:${PORT}`

export default defineConfig({
  testDir: './e2e',
  // Each test owns its own fake backend state, so they are fully isolated and
  // safe to parallelize.
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL,
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  // Boot the production preview server for the SPA. `npm run build` runs first
  // (tsc -b + vite build); preview serves the built assets on PORT. When
  // E2E_BASE_URL is set we assume the app is already running elsewhere.
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: `npm run build && npm run preview -- --port ${PORT} --strictPort`,
        url: baseURL,
        reuseExistingServer: !process.env.CI,
        timeout: 180_000,
      },
})
