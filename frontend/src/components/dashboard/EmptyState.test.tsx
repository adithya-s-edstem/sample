import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import EmptyState from './EmptyState'

/*
 * EmptyState component tests (P7-5). Per docs/testing-plan.md §4 ("empty state
 * shows"): the page-level empty prompt renders the wireframe copy
 * (docs/wireframes/empty.html) and its "Add your first expense" CTA invokes the
 * add handler. ExpenseListSection.test.tsx covers wiring this into the list when a
 * month resolves with no rows; these tests pin the component in isolation.
 */
describe('EmptyState (P7-5)', () => {
  it('renders the wireframe heading, prompt, and CTA', () => {
    render(<EmptyState />)

    expect(screen.getByRole('heading', { name: 'No expenses this month' })).toBeInTheDocument()
    expect(
      screen.getByText('Start tracking where your money goes by adding your first expense.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Add your first expense' })).toBeInTheDocument()
  })

  it('fires onAddExpense when the CTA is clicked', () => {
    const onAddExpense = vi.fn()
    render(<EmptyState onAddExpense={onAddExpense} />)

    fireEvent.click(screen.getByRole('button', { name: 'Add your first expense' }))
    expect(onAddExpense).toHaveBeenCalledTimes(1)
  })

  it('is inert (no throw) when clicked without a handler', () => {
    render(<EmptyState />)
    expect(() =>
      fireEvent.click(screen.getByRole('button', { name: 'Add your first expense' })),
    ).not.toThrow()
  })
})
