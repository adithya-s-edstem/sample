/*
 * Filter bar shell: date range, category, amount range, and search, laid out as
 * the wireframe's pill row. These become controlled inputs bound to the list
 * query in Phase 8 (P8-1); here they are static placeholders.
 */
function FilterBar() {
  const field =
    'inline-flex items-center gap-2 rounded-[10px] border border-line bg-card px-3 py-2.5 text-[13px] text-muted'

  return (
    <div className="mt-6 mb-3.5 flex flex-wrap items-center gap-2.5">
      <span className={field}>📅 Date range</span>
      <span className={field}>Category: All ▼</span>
      <span className={field}>₹ Min – Max</span>
      <span className={`${field} min-w-[180px] flex-1`}>⌕ Search expenses</span>
    </div>
  )
}

export default FilterBar
