/*
 * Non-component module for the month-state context (P5-3): the React context
 * object, its value type, and the `useMonth` accessor. Kept separate from the
 * provider component (MonthContext.tsx) so the provider file only exports a
 * component (Fast Refresh / react-refresh requirement).
 */
import { createContext, useContext } from 'react'
import type { Month, MonthRange } from '../lib/month'

export interface MonthContextValue {
  /** The selected month, normalized to its first day. */
  month: Month
  /** Display label for the header, e.g. `June 2026`. */
  label: string
  /** Inclusive `YYYY-MM-DD` range for the month, fed to all queries as `from`/`to`. */
  range: MonthRange
  /** Whether the selected month is the current (real-world) month. */
  isCurrentMonth: boolean
  /** Jump to an arbitrary month (normalized to its first day). */
  setMonth: (month: Month) => void
  /** Step back one month. */
  goToPreviousMonth: () => void
  /** Step forward one month. */
  goToNextMonth: () => void
  /** Reset to the current month. */
  goToCurrentMonth: () => void
}

export const MonthContext = createContext<MonthContextValue | undefined>(undefined)

/** Read the selected-month state. Must be used inside a `MonthProvider`. */
export function useMonth(): MonthContextValue {
  const ctx = useContext(MonthContext)
  if (ctx === undefined) {
    throw new Error('useMonth must be used within a MonthProvider')
  }
  return ctx
}
