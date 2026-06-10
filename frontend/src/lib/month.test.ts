import { describe, it, expect } from 'vitest'
import { expenseDateLabel, trendPointLabel } from './month'

/*
 * trendPointLabel formats a /summary/trend bucket `period` into the compact
 * x-axis label the wireframe uses (P6-3). Periods are plain calendar values
 * (`YYYY-MM-DD` for day granularity, `YYYY-MM` for month) and must render without
 * timezone shifting.
 */
describe('trendPointLabel', () => {
  it('formats a day bucket as `MMM d`', () => {
    expect(trendPointLabel('2026-06-05', 'day')).toBe('Jun 5')
    expect(trendPointLabel('2026-01-31', 'day')).toBe('Jan 31')
  })

  it('formats a month bucket as `MMM`', () => {
    expect(trendPointLabel('2026-01', 'month')).toBe('Jan')
    expect(trendPointLabel('2026-12', 'month')).toBe('Dec')
  })

  it('uses the calendar date as-is (no off-by-one from timezone)', () => {
    // A first-of-month day bucket must read as that day, not the prior month.
    expect(trendPointLabel('2026-06-01', 'day')).toBe('Jun 1')
  })
})

/*
 * expenseDateLabel formats an expense's `YYYY-MM-DD` date into the table's
 * `d MMM yyyy` label (P7-1), as a plain calendar value with no timezone shift.
 */
describe('expenseDateLabel', () => {
  it('formats a date as `d MMM yyyy`', () => {
    expect(expenseDateLabel('2026-06-10')).toBe('10 Jun 2026')
    expect(expenseDateLabel('2026-06-09')).toBe('9 Jun 2026')
    expect(expenseDateLabel('2026-01-05')).toBe('5 Jan 2026')
  })

  it('uses the calendar date as-is (no off-by-one from timezone)', () => {
    expect(expenseDateLabel('2026-06-01')).toBe('1 Jun 2026')
    expect(expenseDateLabel('2026-12-31')).toBe('31 Dec 2026')
  })
})
