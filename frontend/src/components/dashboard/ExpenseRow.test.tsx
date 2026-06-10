import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import type { ReactNode } from 'react'
import ExpenseRow from './ExpenseRow'
import type { Expense } from '../../api/types'

/*
 * ExpenseRow unit tests (P7-1): a row renders date / category pill / amount /
 * actions from one expense, and the edit/delete buttons fire their callbacks.
 * ExpenseRow is a <tr>, so it's wrapped in a <table><tbody> for valid DOM.
 */

const expense: Expense = {
  id: '11111111-1111-1111-1111-111111111111',
  amount: 1200,
  date: '2026-06-10',
  category: 'GROCERIES',
  createdAt: '2026-06-10T09:30:00Z',
  updatedAt: '2026-06-10T09:30:00Z',
}

function renderRow(ui: ReactNode) {
  return render(
    <table>
      <tbody>{ui}</tbody>
    </table>,
  )
}

describe('ExpenseRow (P7-1)', () => {
  it('renders the date, category pill, and amount with two decimals', () => {
    renderRow(<ExpenseRow expense={expense} />)

    expect(screen.getByText('10 Jun 2026')).toBeInTheDocument()
    expect(screen.getByText('Groceries')).toBeInTheDocument()
    expect(screen.getByText('₹1,200.00')).toBeInTheDocument()
  })

  it('always shows two decimals even for fractional amounts', () => {
    renderRow(<ExpenseRow expense={{ ...expense, amount: 1234.5 }} />)
    expect(screen.getByText('₹1,234.50')).toBeInTheDocument()
  })

  it('renders edit and delete action buttons', () => {
    renderRow(<ExpenseRow expense={expense} />)

    expect(
      screen.getByRole('button', { name: 'Edit expense from 10 Jun 2026' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Delete expense from 10 Jun 2026' }),
    ).toBeInTheDocument()
  })

  it('fires onEdit with the expense when the edit action is clicked', () => {
    const onEdit = vi.fn()
    renderRow(<ExpenseRow expense={expense} onEdit={onEdit} />)

    fireEvent.click(screen.getByRole('button', { name: 'Edit expense from 10 Jun 2026' }))
    expect(onEdit).toHaveBeenCalledTimes(1)
    expect(onEdit).toHaveBeenCalledWith(expense)
  })

  it('fires onDelete with the expense when the delete action is clicked', () => {
    const onDelete = vi.fn()
    renderRow(<ExpenseRow expense={expense} onDelete={onDelete} />)

    fireEvent.click(screen.getByRole('button', { name: 'Delete expense from 10 Jun 2026' }))
    expect(onDelete).toHaveBeenCalledTimes(1)
    expect(onDelete).toHaveBeenCalledWith(expense)
  })
})
