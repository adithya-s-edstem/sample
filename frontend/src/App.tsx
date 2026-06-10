import Header from './components/layout/Header'
import SummaryRow from './components/dashboard/SummaryRow'
import TrendSection from './components/dashboard/TrendSection'
import FilterBar from './components/dashboard/FilterBar'
import ExpenseListSection from './components/dashboard/ExpenseListSection'

/*
 * App shell (P5-1): the single scrollable page from docs/solution.md, themed to
 * match the wireframes. Each region is a structural placeholder; live data,
 * month state, charts, the table, and the modal arrive in P5-2 through P8.
 */
function App() {
  return (
    <main className="min-h-screen bg-bg text-ink">
      <div className="mx-auto max-w-[1200px] px-8 pt-7 pb-14">
        <Header />
        <SummaryRow />
        <TrendSection />
        <FilterBar />
        <ExpenseListSection />
      </div>
    </main>
  )
}

export default App
