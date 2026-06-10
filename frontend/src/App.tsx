import { useState } from 'react'
import Header from './components/layout/Header'
import SummaryRow from './components/dashboard/SummaryRow'
import TrendSection from './components/dashboard/TrendSection'
import FilterBar from './components/dashboard/FilterBar'
import ExpenseListSection from './components/dashboard/ExpenseListSection'
import ExpenseFormModal from './components/dashboard/ExpenseFormModal'
import type { Expense } from './api/types'

/*
 * App shell (P5-1): the single scrollable page from docs/solution.md, themed to
 * match the wireframes.
 *
 * P7-3 wires the Add/Edit flow: the page owns the modal state so every trigger —
 * the header "Add Expense", the empty-state CTA, and each row's edit action —
 * opens the same modal. `null` = closed, `{ mode: 'add' }` = add a new expense,
 * `{ mode: 'edit', expense }` = edit an existing one. The container runs the
 * create/update mutation, which invalidates the list + summaries so the dashboard
 * refreshes live. Delete (P7-4) and the CSV export wiring (P8-3) come later.
 */
type ModalState = { mode: 'add' } | { mode: 'edit'; expense: Expense } | null

function App() {
  const [modal, setModal] = useState<ModalState>(null)

  const openAdd = () => setModal({ mode: 'add' })
  const openEdit = (expense: Expense) => setModal({ mode: 'edit', expense })
  const closeModal = () => setModal(null)

  return (
    <main className="min-h-screen bg-bg text-ink">
      <div className="mx-auto max-w-[1200px] px-8 pt-7 pb-14">
        <Header onAddExpense={openAdd} />
        <SummaryRow />
        <TrendSection />
        <FilterBar />
        <ExpenseListSection onAddExpense={openAdd} onEditExpense={openEdit} />
      </div>
      {modal && (
        <ExpenseFormModal
          expense={modal.mode === 'edit' ? modal.expense : undefined}
          onClose={closeModal}
        />
      )}
    </main>
  )
}

export default App
