import { describe, it, expect } from 'vitest'
import {
  currentMonth,
  expenseDateLabel,
  isSameMonth,
  monthLabel,
  monthRange,
  nextMonth,
  normalizeMonth,
  previousMonth,
  trendPointLabel,
} from './month'

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

/*
 * monthLabel renders the header's `MMMM yyyy` label for a selected month (P5-3).
 * A `Month` is the first-of-month Date; the label is the full month name + year.
 */
describe('monthLabel', () => {
  it('formats a month as `MMMM yyyy`', () => {
    expect(monthLabel(new Date(2026, 5, 1))).toBe('June 2026')
    expect(monthLabel(new Date(2026, 0, 1))).toBe('January 2026')
    expect(monthLabel(new Date(2025, 11, 1))).toBe('December 2025')
  })

  it('uses the month of any day within it (label is month-scoped)', () => {
    // Mid-month and last-day Dates still label as that month/year.
    expect(monthLabel(new Date(2026, 5, 17))).toBe('June 2026')
    expect(monthLabel(new Date(2026, 1, 28))).toBe('February 2026')
  })
})

/*
 * monthRange derives the inclusive `YYYY-MM-DD` from/to range the API expects for
 * a month (P5-3), matching the backend's current-month defaulting contract: from
 * = first day, to = last day, with month-length handled (28/29/30/31).
 */
describe('monthRange', () => {
  it('spans the first to last calendar day of a 30-day month', () => {
    expect(monthRange(new Date(2026, 5, 1))).toEqual({
      from: '2026-06-01',
      to: '2026-06-30',
    })
  })

  it('spans a 31-day month', () => {
    expect(monthRange(new Date(2026, 0, 1))).toEqual({
      from: '2026-01-01',
      to: '2026-01-31',
    })
  })

  it('handles February in a non-leap year (28 days)', () => {
    expect(monthRange(new Date(2026, 1, 1))).toEqual({
      from: '2026-02-01',
      to: '2026-02-28',
    })
  })

  it('handles February in a leap year (29 days)', () => {
    expect(monthRange(new Date(2024, 1, 1))).toEqual({
      from: '2024-02-01',
      to: '2024-02-29',
    })
  })

  it('derives the range from any day within the month, not just the first', () => {
    expect(monthRange(new Date(2026, 5, 17))).toEqual({
      from: '2026-06-01',
      to: '2026-06-30',
    })
  })
})

/*
 * Month navigation/normalization helpers (P5-3): a Month is canonical at the
 * first of the month, and prev/next step across year boundaries.
 */
describe('month helpers', () => {
  it('currentMonth normalizes "now" to the first of the month', () => {
    expect(currentMonth(new Date(2026, 5, 17, 13, 45))).toEqual(new Date(2026, 5, 1))
  })

  it('normalizeMonth snaps any date to the first of its month', () => {
    expect(normalizeMonth(new Date(2026, 5, 30))).toEqual(new Date(2026, 5, 1))
  })

  it('previousMonth steps back a month, crossing the year boundary', () => {
    expect(previousMonth(new Date(2026, 5, 1))).toEqual(new Date(2026, 4, 1))
    expect(previousMonth(new Date(2026, 0, 1))).toEqual(new Date(2025, 11, 1))
  })

  it('nextMonth steps forward a month, crossing the year boundary', () => {
    expect(nextMonth(new Date(2026, 5, 1))).toEqual(new Date(2026, 6, 1))
    expect(nextMonth(new Date(2026, 11, 1))).toEqual(new Date(2027, 0, 1))
  })

  it('isSameMonth compares calendar month + year, ignoring the day', () => {
    expect(isSameMonth(new Date(2026, 5, 1), new Date(2026, 5, 30))).toBe(true)
    expect(isSameMonth(new Date(2026, 5, 1), new Date(2026, 6, 1))).toBe(false)
    expect(isSameMonth(new Date(2026, 5, 1), new Date(2025, 5, 1))).toBe(false)
  })
})
