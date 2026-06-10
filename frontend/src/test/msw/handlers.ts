import { http, HttpResponse } from 'msw'
import type {
  CategorySummaryResponse,
  Category,
  Expense,
  PageResponse,
  SummaryResponse,
  TrendResponse,
} from '../../api/types'

/*
 * Default MSW handlers covering the happy path of every endpoint the hooks call,
 * shaped exactly like docs/api-contracts.md. Paths are relative `/api/...` (jsdom
 * resolves them against the test origin, http://localhost). Money values keep two
 * decimals as JSON numbers, matching the server's BigDecimal/NUMERIC(12,2) output.
 * Tests override individual routes with `server.use(...)` for loading/error cases.
 */

export const sampleExpense: Expense = {
  id: '11111111-1111-1111-1111-111111111111',
  amount: 1234.56,
  date: '2026-06-05',
  category: 'GROCERIES',
  createdAt: '2026-06-05T09:30:00Z',
  updatedAt: '2026-06-05T09:30:00Z',
}

export const sampleExpensePage: PageResponse<Expense> = {
  content: [sampleExpense],
  page: 0,
  size: 50,
  totalElements: 1,
  totalPages: 1,
  sort: 'date,desc',
}

export const sampleSummary: SummaryResponse = {
  from: '2026-06-01',
  to: '2026-06-30',
  total: 1234.56,
  count: 1,
  currency: 'INR',
}

export const sampleByCategory: CategorySummaryResponse = {
  from: '2026-06-01',
  to: '2026-06-30',
  total: 1234.56,
  categories: [{ category: 'GROCERIES', total: 1234.56, count: 1, percent: 100 }],
}

export const sampleTrend: TrendResponse = {
  from: '2026-06-01',
  to: '2026-06-30',
  granularity: 'day',
  points: [{ period: '2026-06-05', total: 1234.56 }],
}

export const sampleCategories: Category[] = [
  'FOOD',
  'TRANSPORT',
  'RENT',
  'UTILITIES',
  'GROCERIES',
  'ENTERTAINMENT',
  'HEALTH',
  'SHOPPING',
  'OTHER',
]

export const handlers = [
  http.get('/api/expenses', () => HttpResponse.json(sampleExpensePage)),
  http.get('/api/expenses/:id', ({ params }) =>
    HttpResponse.json({ ...sampleExpense, id: params.id as string }),
  ),
  http.post('/api/expenses', async ({ request }) => {
    const body = (await request.json()) as Partial<Expense>
    return HttpResponse.json({ ...sampleExpense, ...body }, { status: 201 })
  }),
  http.put('/api/expenses/:id', async ({ params, request }) => {
    const body = (await request.json()) as Partial<Expense>
    return HttpResponse.json({ ...sampleExpense, ...body, id: params.id as string })
  }),
  http.delete('/api/expenses/:id', () => new HttpResponse(null, { status: 204 })),

  http.get('/api/summary', () => HttpResponse.json(sampleSummary)),
  http.get('/api/summary/by-category', () => HttpResponse.json(sampleByCategory)),
  http.get('/api/summary/trend', () => HttpResponse.json(sampleTrend)),

  http.get('/api/categories', () => HttpResponse.json(sampleCategories)),
]
