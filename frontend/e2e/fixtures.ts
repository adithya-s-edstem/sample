import { test as base, type Page, type Route } from '@playwright/test'

/*
 * A deterministic, stateful in-memory fake of the Expense Tracker API, mounted on
 * the page via Playwright route interception (P9-4). It mirrors
 * docs/api-contracts.md exactly so the E2E flows exercise the real SPA against a
 * faithful backend without standing up Postgres + the JVM.
 *
 * It owns the same money/aggregation rules as the server: amounts are exact
 * 2-decimal values, totals are summed in integer paise to avoid float drift,
 * by-category percentages are rounded half-up to 2 decimals from exact totals,
 * the trend is bucketed by day (single-month range) or month, and list/summary/
 * export default to the current month when `from`/`to` are omitted.
 *
 * Each test gets a fresh backend seeded with `seedExpenses`, so tests are
 * isolated and parallel-safe. `backend.startFailing(...)` / `backend.recover(...)`
 * let a test force an endpoint to fail (until recovered) to drive the
 * loading/error + retry flow deterministically past TanStack Query auto-retries.
 */

interface ApiExpense {
  id: string
  amount: number
  date: string
  category: string
  createdAt: string
  updatedAt: string
}

interface ExpenseInput {
  amount: number
  date: string
  category: string
}

const CATEGORIES = [
  'FOOD',
  'TRANSPORT',
  'RENT',
  'UTILITIES',
  'GROCERIES',
  'ENTERTAINMENT',
  'HEALTH',
  'SHOPPING',
  'OTHER',
] as const

/** The current month as `YYYY-MM`, so seeds land in the app's default scope. */
function currentMonthPrefix(now: Date = new Date()): string {
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

/** A `YYYY-MM-DD` date on `day` of the current month. */
export function dayInCurrentMonth(day: number, now: Date = new Date()): string {
  return `${currentMonthPrefix(now)}-${String(day).padStart(2, '0')}`
}

function firstOfMonth(prefix: string): string {
  return `${prefix}-01`
}

function lastOfMonth(prefix: string): string {
  const [y, m] = prefix.split('-').map(Number)
  const last = new Date(y, m, 0).getDate()
  return `${prefix}-${String(last).padStart(2, '0')}`
}

/** Money in exact integer paise to avoid float drift, mirroring NUMERIC(12,2). */
function toPaise(amount: number): number {
  return Math.round(amount * 100)
}

function paiseToNumber(paise: number): number {
  return paise / 100
}

/** Renders a number with exactly two decimals (no scientific notation), like the CSV. */
function twoDecimals(amount: number): string {
  return amount.toFixed(2)
}

let idCounter = 0
function nextId(): string {
  idCounter += 1
  // Deterministic UUID-shaped id; the SPA only needs a unique string.
  const n = String(idCounter).padStart(12, '0')
  return `00000000-0000-4000-8000-${n}`
}

type FailableEndpoint =
  | 'list'
  | 'summary'
  | 'summary/by-category'
  | 'summary/trend'
  | 'create'
  | 'update'
  | 'delete'

class FakeBackend {
  private expenses: ApiExpense[] = []
  private failing = new Set<FailableEndpoint>()

  constructor(seed: ExpenseInput[]) {
    const now = new Date().toISOString()
    this.expenses = seed.map((e) => ({
      id: nextId(),
      amount: e.amount,
      date: e.date,
      category: e.category,
      createdAt: now,
      updatedAt: now,
    }))
  }

  /**
   * Force every request to `endpoint` to fail with a 500 until {@link recover}
   * is called. TanStack Query auto-retries queries, so a one-shot failure would
   * be silently retried away before the error UI shows; keeping the endpoint
   * failing until the test explicitly recovers makes the loading→error→retry
   * flow deterministic regardless of the retry count/backoff.
   */
  startFailing(endpoint: FailableEndpoint) {
    this.failing.add(endpoint)
  }

  /** Stop forcing failures on `endpoint`, so the next request succeeds. */
  recover(endpoint: FailableEndpoint) {
    this.failing.delete(endpoint)
  }

  private shouldFail(endpoint: FailableEndpoint): boolean {
    return this.failing.has(endpoint)
  }

  private resolveRange(params: URLSearchParams): { from: string; to: string } {
    const prefix = currentMonthPrefix()
    return {
      from: params.get('from') ?? firstOfMonth(prefix),
      to: params.get('to') ?? lastOfMonth(prefix),
    }
  }

  private filtered(params: URLSearchParams): ApiExpense[] {
    const { from, to } = this.resolveRange(params)
    const category = params.get('category')
    const minAmount = params.get('minAmount')
    const maxAmount = params.get('maxAmount')
    const q = params.get('q')

    let rows = this.expenses.filter((e) => e.date >= from && e.date <= to)
    if (category) rows = rows.filter((e) => e.category === category)
    if (minAmount !== null)
      rows = rows.filter((e) => toPaise(e.amount) >= toPaise(Number(minAmount)))
    if (maxAmount !== null)
      rows = rows.filter((e) => toPaise(e.amount) <= toPaise(Number(maxAmount)))
    // `q` is reserved free-text search; per api-contracts.md it matches category
    // for now (case-insensitive substring), so the search flow has something to bite on.
    if (q) rows = rows.filter((e) => e.category.toLowerCase().includes(q.toLowerCase()))
    // Default sort: date,desc.
    rows.sort((a, b) => (a.date < b.date ? 1 : a.date > b.date ? -1 : 0))
    return rows
  }

  /** Renders the RFC-4180-ish CSV body the export endpoint returns for `params`. */
  private csv(params: URLSearchParams): string {
    const lines = ['id,date,category,amount']
    for (const e of this.filtered(params)) {
      lines.push(`${e.id},${e.date},${e.category},${twoDecimals(e.amount)}`)
    }
    return lines.join('\n') + '\n'
  }

  /**
   * Test helper: the exact CSV the export endpoint would return for a given
   * export URL (e.g. `download.url()`). Browsers cancel a fulfilled navigation
   * once it becomes a download, so the body can't be read off the download
   * itself — this reproduces it deterministically from the same backend state.
   */
  csvForUrl(url: string): string {
    return this.csv(new URL(url).searchParams)
  }

  async handle(route: Route) {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname.replace(/^\/api/, '')
    const method = request.method()
    const params = url.searchParams

    // --- Reference ---
    if (path === '/categories' && method === 'GET') {
      return route.fulfill({ json: [...CATEGORIES] })
    }

    // --- CRUD ---
    if (path === '/expenses' && method === 'POST') {
      if (this.shouldFail('create')) return this.fail(route, 500, '/api/expenses')
      const body = request.postDataJSON() as ExpenseInput
      const err = this.validate(body, '/api/expenses')
      if (err) return route.fulfill({ status: 400, json: err })
      const now = new Date().toISOString()
      const created: ApiExpense = {
        id: nextId(),
        amount: body.amount,
        date: body.date,
        category: body.category,
        createdAt: now,
        updatedAt: now,
      }
      this.expenses.push(created)
      return route.fulfill({ status: 201, json: created })
    }

    const idMatch = path.match(/^\/expenses\/([^/]+)$/)
    if (idMatch) {
      const id = idMatch[1]
      const idx = this.expenses.findIndex((e) => e.id === id)
      if (method === 'GET') {
        if (idx === -1) return this.fail(route, 404, request.url())
        return route.fulfill({ json: this.expenses[idx] })
      }
      if (method === 'PUT') {
        if (this.shouldFail('update')) return this.fail(route, 500, request.url())
        if (idx === -1) return this.fail(route, 404, request.url())
        const body = request.postDataJSON() as ExpenseInput
        const err = this.validate(body, url.pathname)
        if (err) return route.fulfill({ status: 400, json: err })
        const updated: ApiExpense = {
          ...this.expenses[idx],
          amount: body.amount,
          date: body.date,
          category: body.category,
          updatedAt: new Date().toISOString(),
        }
        this.expenses[idx] = updated
        return route.fulfill({ json: updated })
      }
      if (method === 'DELETE') {
        if (this.shouldFail('delete')) return this.fail(route, 500, request.url())
        if (idx === -1) return this.fail(route, 404, request.url())
        this.expenses.splice(idx, 1)
        return route.fulfill({ status: 204, body: '' })
      }
    }

    // --- Export (CSV) ---
    if (path === '/expenses/export' && method === 'GET') {
      const today = new Date().toISOString().slice(0, 10)
      return route.fulfill({
        status: 200,
        headers: {
          'content-type': 'text/csv; charset=UTF-8',
          'content-disposition': `attachment; filename="expenses-${today}.csv"`,
        },
        body: this.csv(params),
      })
    }

    // --- List ---
    if (path === '/expenses' && method === 'GET') {
      if (this.shouldFail('list')) return this.fail(route, 500, request.url())
      const rows = this.filtered(params)
      const size = Number(params.get('size') ?? '50')
      const page = Number(params.get('page') ?? '0')
      const start = page * size
      const content = rows.slice(start, start + size)
      return route.fulfill({
        json: {
          content,
          page,
          size,
          totalElements: rows.length,
          totalPages: Math.max(1, Math.ceil(rows.length / size)),
          sort: params.get('sort') ?? 'date,desc',
        },
      })
    }

    // --- Summaries ---
    if (path === '/summary' && method === 'GET') {
      if (this.shouldFail('summary')) return this.fail(route, 500, request.url())
      const { from, to } = this.resolveRange(params)
      const rows = this.filtered(params)
      const totalPaise = rows.reduce((sum, e) => sum + toPaise(e.amount), 0)
      return route.fulfill({
        json: {
          from,
          to,
          total: paiseToNumber(totalPaise),
          count: rows.length,
          currency: 'INR',
        },
      })
    }

    if (path === '/summary/by-category' && method === 'GET') {
      if (this.shouldFail('summary/by-category')) return this.fail(route, 500, request.url())
      const { from, to } = this.resolveRange(params)
      const rows = this.filtered(params)
      const byCat = new Map<string, { paise: number; count: number }>()
      for (const e of rows) {
        const cur = byCat.get(e.category) ?? { paise: 0, count: 0 }
        cur.paise += toPaise(e.amount)
        cur.count += 1
        byCat.set(e.category, cur)
      }
      const totalPaise = rows.reduce((sum, e) => sum + toPaise(e.amount), 0)
      const categories = [...byCat.entries()]
        .map(([category, { paise, count }]) => ({
          category,
          total: paiseToNumber(paise),
          count,
          // Half-up to 2 decimals from exact paise, mirroring the server.
          percent: totalPaise === 0 ? 0 : Math.round((paise / totalPaise) * 10000) / 100,
        }))
        .sort((a, b) => b.total - a.total)
      return route.fulfill({
        json: { from, to, total: paiseToNumber(totalPaise), categories },
      })
    }

    if (path === '/summary/trend' && method === 'GET') {
      if (this.shouldFail('summary/trend')) return this.fail(route, 500, request.url())
      const { from, to } = this.resolveRange(params)
      const rows = this.filtered(params)
      // Single-month range defaults to day granularity (matches the backend).
      const sameMonth = from.slice(0, 7) === to.slice(0, 7)
      const granularity = params.get('granularity') ?? (sameMonth ? 'day' : 'month')
      const buckets = new Map<string, number>()
      for (const e of rows) {
        const key = granularity === 'month' ? e.date.slice(0, 7) : e.date
        buckets.set(key, (buckets.get(key) ?? 0) + toPaise(e.amount))
      }
      const points = [...buckets.entries()]
        .map(([period, paise]) => ({ period, total: paiseToNumber(paise) }))
        .sort((a, b) => (a.period < b.period ? -1 : 1))
      return route.fulfill({ json: { from, to, granularity, points } })
    }

    // Anything unmatched: surface loudly so a missing handler fails the test.
    return route.fulfill({
      status: 501,
      json: { message: `Unhandled ${method} ${path}` },
    })
  }

  private validate(body: ExpenseInput, path: string): object | null {
    const fieldErrors: { field: string; message: string }[] = []
    if (body.amount === undefined || body.amount === null) {
      fieldErrors.push({ field: 'amount', message: 'must not be null' })
    } else if (body.amount <= 0) {
      fieldErrors.push({ field: 'amount', message: 'must be greater than 0' })
    }
    if (!body.date) fieldErrors.push({ field: 'date', message: 'must not be null' })
    if (!body.category || !CATEGORIES.includes(body.category as (typeof CATEGORIES)[number])) {
      fieldErrors.push({ field: 'category', message: 'must be a valid category' })
    }
    if (fieldErrors.length === 0) return null
    return {
      timestamp: new Date().toISOString(),
      status: 400,
      error: 'Bad Request',
      message: fieldErrors[0].message,
      path,
      fieldErrors,
    }
  }

  private fail(route: Route, status: number, path: string) {
    return route.fulfill({
      status,
      json: {
        timestamp: new Date().toISOString(),
        status,
        error: status === 404 ? 'Not Found' : 'Internal Server Error',
        message: status === 404 ? 'Expense not found' : 'Something went wrong',
        path,
      },
    })
  }
}

/**
 * A deterministic seed spanning multiple categories within the current month so
 * the dashboard renders fully under the app's default current-month scope.
 * Amounts are exact 2-decimal values; one carries paise to guard formatting.
 */
export function seedExpenses(): ExpenseInput[] {
  return [
    { amount: 12000.0, date: dayInCurrentMonth(1), category: 'RENT' },
    { amount: 5200.5, date: dayInCurrentMonth(5), category: 'GROCERIES' },
    { amount: 3300.0, date: dayInCurrentMonth(8), category: 'FOOD' },
    { amount: 1500.0, date: dayInCurrentMonth(12), category: 'TRANSPORT' },
    { amount: 900.0, date: dayInCurrentMonth(20), category: 'ENTERTAINMENT' },
  ]
}

type Fixtures = {
  backend: FakeBackend
  mountedPage: Page
}

/**
 * The E2E test object: every test receives a `backend` (fresh, seeded) and a
 * `mountedPage` whose `/api/**` calls are routed to that backend. Tests that
 * want a custom seed can re-seed by overriding the `backend` fixture per file.
 */
export const test = base.extend<Fixtures>({
  backend: async ({}, use) => {
    await use(new FakeBackend(seedExpenses()))
  },
  mountedPage: async ({ page, backend }, use) => {
    await page.route('**/api/**', (route) => backend.handle(route))
    await use(page)
  },
})

export { expect } from '@playwright/test'
