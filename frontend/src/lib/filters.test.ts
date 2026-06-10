import { describe, it, expect } from 'vitest'
import { buildExpenseQuery } from './filters'
import { EMPTY_FILTERS, type FilterState } from '../context/filterContext'
import type { MonthRange } from './month'

const JUNE: MonthRange = { from: '2026-06-01', to: '2026-06-30' }

function filters(overrides: Partial<FilterState> = {}): FilterState {
  return { ...EMPTY_FILTERS, ...overrides }
}

describe('buildExpenseQuery (P8-1)', () => {
  it('defaults the date range to the selected month when no date filter is set', () => {
    expect(buildExpenseQuery(filters(), JUNE)).toEqual({ from: '2026-06-01', to: '2026-06-30' })
  })

  it('omits empty category / amount / search filters', () => {
    const query = buildExpenseQuery(filters(), JUNE)
    expect(query.category).toBeUndefined()
    expect(query.minAmount).toBeUndefined()
    expect(query.maxAmount).toBeUndefined()
    expect(query.q).toBeUndefined()
  })

  it('overrides the month range with explicit from/to filters', () => {
    const query = buildExpenseQuery(filters({ from: '2026-06-10', to: '2026-06-20' }), JUNE)
    expect(query.from).toBe('2026-06-10')
    expect(query.to).toBe('2026-06-20')
  })

  it('maps category, amount range, and search onto the query params', () => {
    const query = buildExpenseQuery(
      filters({ category: 'TRANSPORT', minAmount: '100', maxAmount: '500.50', q: 'food' }),
      JUNE,
    )
    expect(query).toEqual({
      from: '2026-06-01',
      to: '2026-06-30',
      category: 'TRANSPORT',
      minAmount: 100,
      maxAmount: 500.5,
      q: 'food',
    })
  })

  it('coerces amounts to numbers and drops non-numeric / blank amounts', () => {
    const query = buildExpenseQuery(filters({ minAmount: '  ', maxAmount: 'abc' }), JUNE)
    expect(query.minAmount).toBeUndefined()
    expect(query.maxAmount).toBeUndefined()
  })

  it('trims the search term and drops a whitespace-only search', () => {
    expect(buildExpenseQuery(filters({ q: '  coffee  ' }), JUNE).q).toBe('coffee')
    expect(buildExpenseQuery(filters({ q: '   ' }), JUNE).q).toBeUndefined()
  })
})
