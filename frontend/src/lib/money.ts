/*
 * Money-formatting helpers for the UI edge (P6-1). Amounts cross the wire as
 * exact two-decimal JSON numbers (server-side BigDecimal / NUMERIC(12,2), INR);
 * we only format them for display here and never do arithmetic on them, so no
 * float drift is introduced. Grouping follows the Indian numbering system
 * (lakh/crore) via the `en-IN` locale, matching the wireframes (e.g. ₹24,500).
 */

/** Indian-grouped number formatter; paise shown only when present. */
const inrFormatter = new Intl.NumberFormat('en-IN', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
})

/**
 * Formats an INR amount for display, e.g. `24500` → `₹24,500` and `1234.5` →
 * `₹1,234.50`. Whole rupee amounts omit the decimals (matching the wireframe);
 * fractional amounts always show two decimals so paise read correctly.
 */
export function formatINR(amount: number): string {
  const hasPaise = !Number.isInteger(amount)
  const formatted = hasPaise
    ? amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : inrFormatter.format(amount)
  return `₹${formatted}`
}

/**
 * Formats an INR amount with always-two decimals, e.g. `1200` → `₹1,200.00` and
 * `1234.5` → `₹1,234.50`. Used in the expense table (P7-1) where every row shows
 * paise for column alignment, matching the dashboard wireframe (`₹1,200.00`).
 * Display-only formatting of the exact two-decimal wire value — no arithmetic.
 */
export function formatINRExact(amount: number): string {
  return `₹${amount.toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}

/** Pluralizes the expense count, e.g. `1 expense`, `42 expenses`, `0 expenses`. */
export function expenseCountLabel(count: number): string {
  return `${count} ${count === 1 ? 'expense' : 'expenses'}`
}
