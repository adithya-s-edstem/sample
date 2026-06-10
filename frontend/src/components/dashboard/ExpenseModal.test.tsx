import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import ExpenseModal from './ExpenseModal'
import type { Expense } from '../../api/types'

/*
 * ExpenseModal component tests (P7-2). Per docs/testing-plan.md §4: the modal
 * renders its fields, enforces required-field + amount > 0 client validation,
 * forwards a clean ExpenseRequest to onSubmit on a valid save, and pre-fills in
 * edit mode. The component is presentational (no network); P7-3 wires mutations.
 *
 * The date defaulting is pinned via fake timers so "defaults to today" is
 * deterministic regardless of when the suite runs.
 */

const expense: Expense = {
  id: '11111111-1111-1111-1111-111111111111',
  amount: 1234.5,
  date: '2026-06-05',
  category: 'GROCERIES',
  createdAt: '2026-06-05T09:30:00Z',
  updatedAt: '2026-06-05T09:30:00Z',
}

describe('ExpenseModal (P7-2)', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-06-10T12:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders the three form fields and add-mode title with date defaulting to today', () => {
    render(<ExpenseModal onSubmit={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByRole('heading', { name: 'Add Expense' })).toBeInTheDocument()
    expect(screen.getByLabelText('Amount (₹)')).toBeInTheDocument()
    expect(screen.getByLabelText('Date')).toHaveValue('2026-06-10')
    expect(screen.getByLabelText('Category')).toBeInTheDocument()
    // All nine fixed categories are offered (plus the disabled placeholder).
    expect(screen.getAllByRole('option')).toHaveLength(10)
  })

  it('shows required-field errors and does not submit when the form is empty', () => {
    const onSubmit = vi.fn()
    render(<ExpenseModal onSubmit={onSubmit} onClose={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: 'Save Expense' }))

    expect(screen.getByText('Amount is required')).toBeInTheDocument()
    expect(screen.getByText('Category is required')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('rejects a zero or negative amount (must be > 0)', () => {
    const onSubmit = vi.fn()
    render(<ExpenseModal onSubmit={onSubmit} onClose={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '0' } })
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'FOOD' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save Expense' }))

    expect(screen.getByText('Amount must be greater than 0')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('rejects an amount with more than two decimals', () => {
    const onSubmit = vi.fn()
    render(<ExpenseModal onSubmit={onSubmit} onClose={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '12.345' } })
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'FOOD' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save Expense' }))

    expect(screen.getByText('Enter a valid amount (up to 2 decimals)')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('accepts 0.01 as the minimum valid amount', () => {
    const onSubmit = vi.fn()
    render(<ExpenseModal onSubmit={onSubmit} onClose={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '0.01' } })
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'FOOD' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save Expense' }))

    expect(onSubmit).toHaveBeenCalledWith({ amount: 0.01, date: '2026-06-10', category: 'FOOD' })
  })

  it('submits a valid request with the exact two-decimal amount', () => {
    const onSubmit = vi.fn()
    render(<ExpenseModal onSubmit={onSubmit} onClose={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '1200.50' } })
    fireEvent.change(screen.getByLabelText('Date'), { target: { value: '2026-06-08' } })
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'RENT' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save Expense' }))

    expect(onSubmit).toHaveBeenCalledTimes(1)
    expect(onSubmit).toHaveBeenCalledWith({
      amount: 1200.5,
      date: '2026-06-08',
      category: 'RENT',
    })
  })

  it('pre-fills the form from the expense in edit mode and updates it', () => {
    const onSubmit = vi.fn()
    render(<ExpenseModal expense={expense} onSubmit={onSubmit} onClose={vi.fn()} />)

    expect(screen.getByRole('heading', { name: 'Edit Expense' })).toBeInTheDocument()
    expect(screen.getByLabelText('Amount (₹)')).toHaveValue('1234.5')
    expect(screen.getByLabelText('Date')).toHaveValue('2026-06-05')
    expect(screen.getByLabelText('Category')).toHaveValue('GROCERIES')

    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '1500' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save Expense' }))

    expect(onSubmit).toHaveBeenCalledWith({
      amount: 1500,
      date: '2026-06-05',
      category: 'GROCERIES',
    })
  })

  it('clears a field error live once the user corrects the input after a submit attempt', () => {
    render(<ExpenseModal onSubmit={vi.fn()} onClose={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: 'Save Expense' }))
    expect(screen.getByText('Amount is required')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Amount (₹)'), { target: { value: '50' } })
    expect(screen.queryByText('Amount is required')).not.toBeInTheDocument()
  })

  it('calls onClose from Cancel, Escape, and an overlay click', () => {
    const onClose = vi.fn()
    const { container } = render(<ExpenseModal onSubmit={vi.fn()} onClose={onClose} />)

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(onClose).toHaveBeenCalledTimes(1)

    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledTimes(2)

    // The overlay is the dialog's backdrop (outermost element).
    fireEvent.mouseDown(container.firstChild as Element)
    expect(onClose).toHaveBeenCalledTimes(3)
  })

  it('disables the actions while submitting', () => {
    render(<ExpenseModal onSubmit={vi.fn()} onClose={vi.fn()} submitting />)

    expect(screen.getByRole('button', { name: 'Saving…' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled()
  })

  it('shows a server-side error banner when provided (P7-3)', () => {
    render(
      <ExpenseModal
        onSubmit={vi.fn()}
        onClose={vi.fn()}
        serverError="amount must be greater than 0"
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('amount must be greater than 0')
  })
})
