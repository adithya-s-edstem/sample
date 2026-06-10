import { useMonth } from '../../context/monthContext'

/*
 * App header: title, month selector, and the primary "Add Expense" action.
 * The month selector (P5-3) reads/mutates the shared month state: prev/next
 * step the month and the label shows the selected month; this scope feeds the
 * summary, trend, and list queries below. The Add action opens the add-expense
 * modal via `onAddExpense` (wired in App, P7-3).
 */
type HeaderProps = {
  /** Opens the add-expense modal. */
  onAddExpense?: () => void
}

function Header({ onAddExpense }: HeaderProps) {
  const { label, goToPreviousMonth, goToNextMonth } = useMonth()

  const navBtn =
    'inline-flex size-9 cursor-pointer items-center justify-center rounded-[10px] border border-line bg-card text-sm text-ink shadow-card'

  return (
    <header className="mb-6 flex items-center justify-between">
      <h1 className="text-xl font-bold tracking-tight">Expense Tracker</h1>
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2">
          <button
            type="button"
            className={navBtn}
            onClick={goToPreviousMonth}
            aria-label="Previous month"
          >
            ‹
          </button>
          <span
            className="inline-flex min-w-[140px] items-center justify-center rounded-[10px] border border-line bg-card px-3.5 py-2.5 text-sm font-medium text-ink shadow-card"
            aria-live="polite"
          >
            {label}
          </span>
          <button type="button" className={navBtn} onClick={goToNextMonth} aria-label="Next month">
            ›
          </button>
        </div>
        <button
          type="button"
          onClick={onAddExpense}
          className="inline-flex cursor-pointer items-center gap-2 rounded-[10px] border-none bg-accent px-4 py-2.5 text-sm font-semibold text-white shadow-card"
        >
          <span className="text-base leading-none">+</span> Add Expense
        </button>
      </div>
    </header>
  )
}

export default Header
