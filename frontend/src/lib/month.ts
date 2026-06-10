/*
 * Month-state helpers (P5-3). The dashboard is scoped to a single calendar month
 * at a time, defaulting to the current month. A month is represented compactly as
 * its first-of-month `Date` (local time); these pure helpers derive everything the
 * UI and queries need from it: a display label, the inclusive `YYYY-MM-DD` range
 * passed to the API (`from`/`to`), and prev/next navigation.
 *
 * Dates are formatted with date-fns. The range maps to the backend's current-month
 * defaulting contract (docs/api-contracts.md): `from` = first day, `to` = last day
 * of the month, both as calendar dates with no time component.
 */
import { addMonths, endOfMonth, format, startOfMonth } from 'date-fns'

/** A selected month, normalized to the first day of that month (local time). */
export type Month = Date

/** Inclusive calendar-date range for a month, as `YYYY-MM-DD` strings. */
export interface MonthRange {
  from: string
  to: string
}

/** The current calendar month (today), normalized to its first day. */
export function currentMonth(now: Date = new Date()): Month {
  return startOfMonth(now)
}

/** Normalizes any date to the first day of its month so a `Month` is canonical. */
export function normalizeMonth(date: Date): Month {
  return startOfMonth(date)
}

/** Human-readable label for the header, e.g. `June 2026`. */
export function monthLabel(month: Month): string {
  return format(month, 'MMMM yyyy')
}

/**
 * Inclusive `YYYY-MM-DD` range covering the whole month. Feeds the list and
 * summary queries as `from`/`to`; matches the backend's current-month default.
 */
export function monthRange(month: Month): MonthRange {
  return {
    from: format(startOfMonth(month), 'yyyy-MM-dd'),
    to: format(endOfMonth(month), 'yyyy-MM-dd'),
  }
}

/** The month immediately before `month` (first day). */
export function previousMonth(month: Month): Month {
  return startOfMonth(addMonths(month, -1))
}

/** The month immediately after `month` (first day). */
export function nextMonth(month: Month): Month {
  return startOfMonth(addMonths(month, 1))
}

/** True when two `Month`s refer to the same calendar month. */
export function isSameMonth(a: Month, b: Month): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth()
}
