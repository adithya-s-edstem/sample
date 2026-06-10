import { CATEGORIES } from '../../api/types'
import { categoryLabel } from '../../lib/category'
import { useFilters } from '../../context/filterContext'

/*
 * Filter bar (P8-1): controlled inputs for date range, category, amount range,
 * and search, laid out as the wireframe's pill row (docs/solution.md §2,
 * docs/wireframes/dashboard.html). Each input is bound to the FilterProvider
 * state; the expense list reads that state and folds it into the
 * `GET /api/expenses` query params (date range, category, minAmount, maxAmount,
 * q). The selected month supplies the default range; the date-range inputs here
 * override it when set.
 *
 * P8-2 will extend the same state to the charts/summary; P8-3 adds the CSV
 * export hitting the same filters. A "Clear" affordance resets all filters.
 */
function FilterBar() {
  const { filters, setFilters, clearFilters, hasActiveFilters } = useFilters()

  const field =
    'inline-flex items-center gap-2 rounded-[10px] border border-line bg-card px-3 py-2.5 text-[13px] text-ink focus-within:border-accent'
  const input = 'bg-transparent text-[13px] text-ink outline-none placeholder:text-muted'

  return (
    <div className="mt-6 mb-3.5 flex flex-wrap items-center gap-2.5">
      <span className={field}>
        <span aria-hidden="true">📅</span>
        <input
          type="date"
          aria-label="From date"
          className={input}
          value={filters.from}
          max={filters.to || undefined}
          onChange={(e) => setFilters({ from: e.target.value })}
        />
      </span>
      <span className="text-[13px] text-muted">–</span>
      <span className={field}>
        <input
          type="date"
          aria-label="To date"
          className={input}
          value={filters.to}
          min={filters.from || undefined}
          onChange={(e) => setFilters({ to: e.target.value })}
        />
      </span>

      <label className={field}>
        <span className="text-muted">Category:</span>
        <select
          aria-label="Filter by category"
          className={`${input} cursor-pointer`}
          value={filters.category}
          onChange={(e) => setFilters({ category: e.target.value as typeof filters.category })}
        >
          <option value="">All</option>
          {CATEGORIES.map((category) => (
            <option key={category} value={category}>
              {categoryLabel(category)}
            </option>
          ))}
        </select>
      </label>

      <span className={field}>
        <span className="text-muted">₹</span>
        <input
          type="number"
          inputMode="decimal"
          min={0}
          step="0.01"
          aria-label="Minimum amount"
          placeholder="Min"
          className={`${input} w-16`}
          value={filters.minAmount}
          onChange={(e) => setFilters({ minAmount: e.target.value })}
        />
        <span className="text-muted">–</span>
        <input
          type="number"
          inputMode="decimal"
          min={0}
          step="0.01"
          aria-label="Maximum amount"
          placeholder="Max"
          className={`${input} w-16`}
          value={filters.maxAmount}
          onChange={(e) => setFilters({ maxAmount: e.target.value })}
        />
      </span>

      <label className={`${field} min-w-[180px] flex-1`}>
        <span aria-hidden="true">⌕</span>
        <input
          type="search"
          aria-label="Search expenses"
          placeholder="Search expenses"
          className={`${input} w-full`}
          value={filters.q}
          onChange={(e) => setFilters({ q: e.target.value })}
        />
      </label>

      {hasActiveFilters && (
        <button
          type="button"
          onClick={clearFilters}
          className="cursor-pointer rounded-[10px] border border-line bg-card px-3 py-2.5 text-[13px] font-medium text-muted hover:text-ink"
        >
          Clear
        </button>
      )}
    </div>
  )
}

export default FilterBar
