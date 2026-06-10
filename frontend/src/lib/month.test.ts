import { describe, it, expect } from 'vitest'
import { trendPointLabel } from './month'

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
