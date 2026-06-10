/*
 * App header: title, month selector, and the primary "Add Expense" action.
 * Per P5-1 this is the static shell that matches the wireframe; the month
 * selector becomes interactive in P5-3 and Add opens the modal in P7.
 */
function Header() {
  return (
    <header className="mb-6 flex items-center justify-between">
      <h1 className="text-xl font-bold tracking-tight">Expense Tracker</h1>
      <div className="flex items-center gap-3">
        <button
          type="button"
          className="inline-flex cursor-pointer items-center gap-2 rounded-[10px] border border-line bg-card px-3.5 py-2.5 text-sm text-ink shadow-card"
        >
          June 2026 <span className="text-[11px] text-muted">▼</span>
        </button>
        <button
          type="button"
          className="inline-flex cursor-pointer items-center gap-2 rounded-[10px] border-none bg-accent px-4 py-2.5 text-sm font-semibold text-white shadow-card"
        >
          <span className="text-base leading-none">+</span> Add Expense
        </button>
      </div>
    </header>
  )
}

export default Header
