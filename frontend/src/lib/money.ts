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

/** Pluralizes the expense count, e.g. `1 expense`, `42 expenses`, `0 expenses`. */
export function expenseCountLabel(count: number): string {
  return `${count} ${count === 1 ? 'expense' : 'expenses'}`
}
