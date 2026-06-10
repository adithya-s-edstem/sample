import { test, expect } from './fixtures'

/*
 * E2E for the export and resilience flows from docs/testing-plan.md §5.6–5.7:
 * the CSV download for the active filter set, and the loading skeleton → error →
 * retry path when the API fails.
 */

test.describe('Export CSV (testing-plan §5.6)', () => {
  test('Export CSV downloads the current (filtered) set with 2-decimal amounts', async ({
    mountedPage,
    backend,
  }) => {
    const page = mountedPage
    await page.goto('/')
    await expect(page.getByRole('row', { name: /Rent/ })).toBeVisible()

    // Filter to RENT so the export reflects exactly the on-screen scope.
    await page.getByLabel('Filter by category').selectOption('RENT')
    await expect(page.getByRole('row', { name: /Groceries/ })).toHaveCount(0)

    // Clicking Export triggers a browser download whose URL carries the active
    // filter (category=RENT plus the month range) — proving the button hits the
    // export endpoint with the current filters.
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'Export CSV' }).click(),
    ])
    const exportUrl = new URL(download.url())
    expect(exportUrl.pathname).toBe('/api/expenses/export')
    expect(exportUrl.searchParams.get('category')).toBe('RENT')
    expect(exportUrl.searchParams.get('from')).toMatch(/^\d{4}-\d{2}-01$/)

    // Browsers cancel a fulfilled navigation once it becomes a download, so the
    // body can't be read off the download. Reproduce it from the same backend
    // state for the exact export URL: only the RENT row, amount with exactly two
    // decimals, no scientific notation.
    const csv = backend.csvForUrl(download.url()).trim()
    const lines = csv.split('\n')
    expect(lines[0]).toBe('id,date,category,amount')
    const dataLines = lines.slice(1)
    expect(dataLines).toHaveLength(1)
    expect(dataLines[0]).toMatch(/,RENT,12000\.00$/)
    expect(csv).not.toMatch(/[eE][+-]\d/)
  })

  test('Export CSV with no filters exports the whole month with paise preserved', async ({
    mountedPage,
    backend,
  }) => {
    const page = mountedPage
    await page.goto('/')
    await expect(page.getByRole('row', { name: /Rent/ })).toBeVisible()

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'Export CSV' }).click(),
    ])

    const csv = backend.csvForUrl(download.url()).trim()
    const dataLines = csv.split('\n').slice(1)
    expect(dataLines).toHaveLength(5)
    // The paise-bearing GROCERIES amount round-trips exactly with two decimals.
    expect(dataLines).toContainEqual(expect.stringMatching(/,GROCERIES,5200\.50$/))
    expect(csv).not.toMatch(/[eE][+-]\d/)
  })
})

test.describe('Loading / error + retry (testing-plan §5.7)', () => {
  test('a failed list shows a graceful error then recovers on Retry', async ({
    mountedPage,
    backend,
  }) => {
    const page = mountedPage

    // Keep the expense-list endpoint failing through the initial query (and its
    // auto-retries), so the graceful error UI is reached deterministically.
    backend.startFailing('list')
    await page.goto('/')

    // Skeletons show during the fetch (testing-plan §5.7); the list renders
    // aria-busy skeleton rows while the query is pending + retrying.
    await expect(page.locator('tr[aria-busy="true"]').first()).toBeVisible()

    // Graceful error + Retry surface in the list region (after retries exhaust).
    const error = page.getByText("Couldn't load expenses")
    await expect(error).toBeVisible({ timeout: 20_000 })

    // Recover the backend, then the user's explicit Retry succeeds.
    backend.recover('list')
    await page.getByRole('button', { name: 'Retry' }).last().click()

    await expect(page.getByRole('row', { name: /Rent/ })).toBeVisible()
    await expect(error).toHaveCount(0)
  })

  test('a failed summary shows an error + retry in the total card', async ({
    mountedPage,
    backend,
  }) => {
    const page = mountedPage

    backend.startFailing('summary')
    await page.goto('/')

    const card = page.locator('section', { hasText: 'This Month' })
    await expect(card.getByRole('alert')).toBeVisible({ timeout: 20_000 })

    backend.recover('summary')
    await card.getByRole('button', { name: 'Retry' }).click()

    await expect(card.getByText('₹22,900.50')).toBeVisible()
  })
})
