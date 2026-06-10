/*
 * Selected-month provider (P5-3). A single source of truth for which calendar
 * month the dashboard is scoped to, defaulting to the current month. The header
 * reads and mutates it (prev/next/jump); the summary, trend, and list sections
 * read its `range` and pass it to their queries as `from`/`to`, so the whole page
 * moves in lockstep when the month changes.
 *
 * The context object and the `useMonth` hook live in ./monthContext (a non-
 * component module) so this file exports only a component.
 */
import { useMemo, useState, type ReactNode } from 'react'
import {
  currentMonth,
  monthLabel,
  monthRange,
  nextMonth as nextMonthOf,
  normalizeMonth,
  previousMonth as previousMonthOf,
  type Month,
} from '../lib/month'
import { MonthContext, type MonthContextValue } from './monthContext'

interface MonthProviderProps {
  children: ReactNode
  /** Optional initial month (mainly for tests); defaults to the current month. */
  initialMonth?: Month
}

export function MonthProvider({ children, initialMonth }: MonthProviderProps) {
  const [month, setMonthState] = useState<Month>(() =>
    initialMonth ? normalizeMonth(initialMonth) : currentMonth(),
  )

  const value = useMemo<MonthContextValue>(() => {
    return {
      month,
      label: monthLabel(month),
      range: monthRange(month),
      isCurrentMonth: month.getTime() === currentMonth().getTime(),
      setMonth: (next: Month) => setMonthState(normalizeMonth(next)),
      goToPreviousMonth: () => setMonthState((m) => previousMonthOf(m)),
      goToNextMonth: () => setMonthState((m) => nextMonthOf(m)),
      goToCurrentMonth: () => setMonthState(currentMonth()),
    }
  }, [month])

  return <MonthContext.Provider value={value}>{children}</MonthContext.Provider>
}
