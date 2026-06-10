import { useState } from 'react'
import Header from './components/layout/Header'
import SummaryRow from './components/dashboard/SummaryRow'
import TrendSection from './components/dashboard/TrendSection'
import FilterBar from './components/dashboard/FilterBar'
import ExpenseListSection from './components/dashboard/ExpenseListSection'
import ExpenseFormModal from './components/dashboard/ExpenseFormModal'
import DeleteExpenseDialog from './components/dashboard/DeleteExpenseDialog'
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
 * refreshes live.
 *
 * P7-4 wires Delete: a row's 🗑 action opens a small confirm prompt
 * (DeleteExpenseDialog) before the API delete runs; on confirm the list +
 * summaries invalidate so the dashboard updates live. The add/edit modal and the
 * delete confirm are mutually exclusive states. CSV export wiring (P8-3) comes
 * later.
 */
type ModalState = { mode: 'add' } | { mode: 'edit'; expense: Expense } | null

function App() {
  const [modal, setModal] = useState<ModalState>(null)
  const [deleting, setDeleting] = useState<Expense | null>(null)

  const openAdd = () => setModal({ mode: 'add' })
  const openEdit = (expense: Expense) => setModal({ mode: 'edit', expense })
  const closeModal = () => setModal(null)

  const openDelete = (expense: Expense) => setDeleting(expense)
  const closeDelete = () => setDeleting(null)

  return (
    <div className="min-h-screen bg-bg text-ink">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <div className="mx-auto max-w-[1200px] px-4 pt-7 pb-14 sm:px-6 lg:px-8">
        <Header onAddExpense={openAdd} />
        <main id="main-content">
          <SummaryRow />
          <TrendSection />
          <FilterBar />
          <ExpenseListSection
            onAddExpense={openAdd}
            onEditExpense={openEdit}
            onDeleteExpense={openDelete}
          />
        </main>
      </div>
      {modal && (
        <ExpenseFormModal
          expense={modal.mode === 'edit' ? modal.expense : undefined}
          onClose={closeModal}
        />
      )}
      {deleting && <DeleteExpenseDialog expense={deleting} onClose={closeDelete} />}
    </div>
  )
}

export default App
