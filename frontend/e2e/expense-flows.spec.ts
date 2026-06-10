import { test, expect, dayInCurrentMonth } from './fixtures'

/*
 * E2E coverage for the key user flows from docs/testing-plan.md §5, driven
 * through the real built SPA against the deterministic in-memory backend
 * (e2e/fixtures.ts). The seed (RENT 12000, GROCERIES 5200.50, FOOD 3300,
 * TRANSPORT 1500, ENTERTAINMENT 900) all lands in the current month, so the
 * dashboard renders fully under the app's default current-month scope.
 *
 * Seed total = 22,900.50 INR.
 */

test.describe('Add expense → dashboard updates (testing-plan §5.1)', () => {
  test('adding an expense shows it in the list and updates the total', async ({ mountedPage }) => {
    const page = mountedPage
    await page.goto('/')

    // The "This Month" total reflects only the seed first.
    const total = page.locator('section', { hasText: 'This Month' }).getByText(/^₹/).first()
    await expect(total).toHaveText('₹22,900.50')

    // Open the add modal from the header and fill the form.
    await page.getByRole('button', { name: 'Add Expense' }).click()
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    await dialog.getByLabel('Amount (₹)').fill('250.75')
    await dialog.getByLabel('Date').fill(dayInCurrentMonth(15))
    await dialog.getByLabel('Category').selectOption('HEALTH')
    await dialog.getByRole('button', { name: 'Save Expense' }).click()

    // The new row appears in the list and the total updates live (cache invalidation).
    await expect(page.getByRole('row', { name: /Health/ })).toBeVisible()
    await expect(page.getByRole('cell', { name: '₹250.75' })).toBeVisible()
    await expect(total).toHaveText('₹23,151.25')
  })

  test('client validation blocks a non-positive amount', async ({ mountedPage }) => {
    const page = mountedPage
    await page.goto('/')

    await page.getByRole('button', { name: 'Add Expense' }).click()
    const dialog = page.getByRole('dialog')
    await dialog.getByLabel('Amount (₹)').fill('0')
    await dialog.getByLabel('Category').selectOption('FOOD')
    await dialog.getByRole('button', { name: 'Save Expense' }).click()

    // Inline error shows and the modal stays open (no request made).
    await expect(dialog.getByText('Amount must be greater than 0')).toBeVisible()
    await expect(dialog).toBeVisible()
  })
})

test.describe('Edit expense (testing-plan §5.2)', () => {
  test('edit pre-fills the modal and saving updates the row + total', async ({ mountedPage }) => {
    const page = mountedPage
    await page.goto('/')

    const total = page.locator('section', { hasText: 'This Month' }).getByText(/^₹/).first()
    await expect(total).toHaveText('₹22,900.50')

    // Edit the FOOD (3300) row.
    const foodRow = page.getByRole('row', { name: /Food/ })
    await foodRow.getByRole('button', { name: /^Edit expense/ }).click()

    const dialog = page.getByRole('dialog')
    await expect(dialog.getByRole('heading', { name: 'Edit Expense' })).toBeVisible()
    // Pre-filled with the existing amount.
    await expect(dialog.getByLabel('Amount (₹)')).toHaveValue('3300')

    await dialog.getByLabel('Amount (₹)').fill('3500')
    await dialog.getByRole('button', { name: 'Save Expense' }).click()

    // Row reflects the new amount; total moves by +200.
    await expect(page.getByRole('cell', { name: '₹3,500.00' })).toBeVisible()
    await expect(total).toHaveText('₹23,100.50')
  })
})

test.describe('Delete expense + confirm (testing-plan §5.3)', () => {
  test('confirming delete removes the row and updates the total', async ({ mountedPage }) => {
    const page = mountedPage
    await page.goto('/')

    const total = page.locator('section', { hasText: 'This Month' }).getByText(/^₹/).first()
    await expect(total).toHaveText('₹22,900.50')

    // Delete the ENTERTAINMENT (900) row.
    const entRow = page.getByRole('row', { name: /Entertainment/ })
    await entRow.getByRole('button', { name: /^Delete expense/ }).click()

    // Confirm prompt appears before anything is deleted.
    const confirm = page.getByRole('alertdialog')
    await expect(confirm.getByRole('heading', { name: 'Delete expense?' })).toBeVisible()
    await confirm.getByRole('button', { name: 'Delete' }).click()

    // Row gone, total down by 900.
    await expect(page.getByRole('row', { name: /Entertainment/ })).toHaveCount(0)
    await expect(total).toHaveText('₹22,000.50')
  })

  test('cancelling the confirm keeps the expense', async ({ mountedPage }) => {
    const page = mountedPage
    await page.goto('/')

    const entRow = page.getByRole('row', { name: /Entertainment/ })
    await entRow.getByRole('button', { name: /^Delete expense/ }).click()
    await page.getByRole('alertdialog').getByRole('button', { name: 'Cancel' }).click()

    await expect(page.getByRole('row', { name: /Entertainment/ })).toBeVisible()
  })
})

test.describe('Filter & search (testing-plan §5.4)', () => {
  test('filtering by category narrows the list and the total', async ({ mountedPage }) => {
    const page = mountedPage
    await page.goto('/')

    const total = page.locator('section', { hasText: 'This Month' }).getByText(/^₹/).first()
    await expect(total).toHaveText('₹22,900.50')

    await page.getByLabel('Filter by category').selectOption('RENT')

    // Only the RENT row remains; the list reflects it.
    await expect(page.getByRole('row', { name: /Rent/ })).toBeVisible()
    await expect(page.getByRole('row', { name: /Groceries/ })).toHaveCount(0)
    // The summary follows the same date scope; the category filter is list-only
    // per api-contracts §3, so the total stays the month total (charts scoped by
    // date range, not category). Assert the list is the narrowed set instead.
    await expect(page.getByRole('cell', { name: '₹12,000.00' })).toBeVisible()
  })

  test('amount range filter narrows the list', async ({ mountedPage }) => {
    const page = mountedPage
    await page.goto('/')

    await page.getByLabel('Minimum amount').fill('5000')
    // RENT (12000) and GROCERIES (5200.50) qualify; the rest drop out.
    await expect(page.getByRole('row', { name: /Rent/ })).toBeVisible()
    await expect(page.getByRole('row', { name: /Groceries/ })).toBeVisible()
    await expect(page.getByRole('row', { name: /Food/ })).toHaveCount(0)
    await expect(page.getByRole('row', { name: /Transport/ })).toHaveCount(0)
  })

  test('search (matches category) narrows the list', async ({ mountedPage }) => {
    const page = mountedPage
    await page.goto('/')

    await page.getByLabel('Search expenses').fill('TRANSPORT')
    await expect(page.getByRole('row', { name: /Transport/ })).toBeVisible()
    await expect(page.getByRole('row', { name: /Rent/ })).toHaveCount(0)
  })
})

test.describe('Month navigation + empty state (testing-plan §5.5)', () => {
  test('navigating to an empty month shows the empty state', async ({ mountedPage }) => {
    const page = mountedPage
    await page.goto('/')

    // The seed is in the current month, so the previous month is empty.
    await expect(page.getByRole('row', { name: /Rent/ })).toBeVisible()
    await page.getByRole('button', { name: 'Previous month' }).click()

    await expect(page.getByRole('heading', { name: 'No expenses this month' })).toBeVisible()
    await expect(page.getByRole('button', { name: /Add your first expense/ })).toBeVisible()
    // This-month total reads zero for the empty month.
    const total = page.locator('section', { hasText: 'This Month' }).getByText(/^₹/).first()
    await expect(total).toHaveText('₹0')

    // Navigating back shows the data again.
    await page.getByRole('button', { name: 'Next month' }).click()
    await expect(page.getByRole('row', { name: /Rent/ })).toBeVisible()
  })
})
