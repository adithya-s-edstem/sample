import { http } from './client'
import type {
  CategorySummaryResponse,
  Category,
  Expense,
  ExpenseQuery,
  ExpenseRequest,
  PageResponse,
  SummaryQuery,
  SummaryResponse,
  TrendQuery,
  TrendResponse,
} from './types'

/*
 * Thin typed wrappers over the REST endpoints in docs/api-contracts.md. Each
 * returns the parsed response body. Query objects are passed straight through as
 * axios `params`; `undefined` fields are omitted from the querystring so the
 * backend applies its defaults (e.g. current-month range, default sort/size).
 */

// --- CRUD ---

export async function createExpense(body: ExpenseRequest): Promise<Expense> {
  const { data } = await http.post<Expense>('/expenses', body)
  return data
}

export async function getExpense(id: string): Promise<Expense> {
  const { data } = await http.get<Expense>(`/expenses/${id}`)
  return data
}

export async function updateExpense(id: string, body: ExpenseRequest): Promise<Expense> {
  const { data } = await http.put<Expense>(`/expenses/${id}`, body)
  return data
}

export async function deleteExpense(id: string): Promise<void> {
  await http.delete(`/expenses/${id}`)
}

// --- List ---

export async function listExpenses(query: ExpenseQuery = {}): Promise<PageResponse<Expense>> {
  const { data } = await http.get<PageResponse<Expense>>('/expenses', { params: query })
  return data
}

// --- Summaries ---

export async function getSummary(query: SummaryQuery = {}): Promise<SummaryResponse> {
  const { data } = await http.get<SummaryResponse>('/summary', { params: query })
  return data
}

export async function getSummaryByCategory(
  query: SummaryQuery = {},
): Promise<CategorySummaryResponse> {
  const { data } = await http.get<CategorySummaryResponse>('/summary/by-category', {
    params: query,
  })
  return data
}

export async function getSummaryTrend(query: TrendQuery = {}): Promise<TrendResponse> {
  const { data } = await http.get<TrendResponse>('/summary/trend', { params: query })
  return data
}

// --- Reference ---

export async function listCategories(): Promise<Category[]> {
  const { data } = await http.get<Category[]>('/categories')
  return data
}

/**
 * Builds the absolute URL for the CSV export endpoint with the given filters
 * applied as querystring params. The export is a file download, so callers point
 * the browser at this URL (e.g. an anchor href) rather than fetching JSON.
 */
export function exportExpensesUrl(query: ExpenseQuery = {}): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      params.append(key, String(value))
    }
  }
  const qs = params.toString()
  return qs ? `/api/expenses/export?${qs}` : '/api/expenses/export'
}
