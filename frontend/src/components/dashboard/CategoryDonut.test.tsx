import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import CategoryDonut from './CategoryDonut'
import type { CategorySummaryResponse } from '../../api/types'

/*
 * CategoryDonut renders /summary/by-category as a donut + legend (P6-2). The
 * legend is the deterministic, assertable surface (the SVG donut renders under
 * jsdom because the PieChart is fixed-size, but its geometry isn't worth
 * asserting); we verify the legend rows, formatting, ordering, and empty state.
 */
const multi: CategorySummaryResponse = {
  from: '2026-06-01',
  to: '2026-06-30',
  total: 18000,
  categories: [
    { category: 'RENT', total: 12000, count: 1, percent: 66.67 },
    { category: 'GROCERIES', total: 5200, count: 3, percent: 28.89 },
    { category: 'FOOD', total: 800, count: 2, percent: 4.44 },
  ],
}

describe('CategoryDonut (P6-2)', () => {
  it('renders a labelled donut chart', () => {
    render(<CategoryDonut data={multi} />)
    expect(screen.getByRole('img', { name: 'Spending by category' })).toBeInTheDocument()
  })

  it('renders one legend row per category with Title-case label, INR amount, and percent', () => {
    render(<CategoryDonut data={multi} />)

    const items = screen.getAllByRole('listitem')
    expect(items).toHaveLength(3)

    // largest-first ordering preserved from the server response
    expect(items[0]).toHaveTextContent('Rent')
    expect(items[0]).toHaveTextContent('₹12,000 · 66.67%')
    expect(items[1]).toHaveTextContent('Groceries')
    expect(items[1]).toHaveTextContent('₹5,200 · 28.89%')
    expect(items[2]).toHaveTextContent('Food')
    expect(items[2]).toHaveTextContent('₹800 · 4.44%')
  })

  it('formats paise exactly with two decimals (no float drift)', () => {
    const data: CategorySummaryResponse = {
      from: '2026-06-01',
      to: '2026-06-30',
      total: 1234.56,
      categories: [{ category: 'FOOD', total: 1234.56, count: 1, percent: 100 }],
    }
    render(<CategoryDonut data={data} />)
    expect(screen.getByText('₹1,234.56 · 100%')).toBeInTheDocument()
  })

  it('shows an empty message when the month has no spending', () => {
    const empty: CategorySummaryResponse = {
      from: '2026-06-01',
      to: '2026-06-30',
      total: 0,
      categories: [],
    }
    render(<CategoryDonut data={empty} />)
    expect(screen.getByText('No spending this month.')).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })

  it('renders the donut + legend (not the empty message) for a zero-total category', () => {
    // Zero state vs. empty state: a category is present in the breakdown but its
    // total/percent are zero. The donut + legend still render (the empty message
    // is reserved for `categories: []`); the legend shows a clean ₹0 · 0%.
    const zero: CategorySummaryResponse = {
      from: '2026-06-01',
      to: '2026-06-30',
      total: 0,
      categories: [{ category: 'FOOD', total: 0, count: 0, percent: 0 }],
    }
    render(<CategoryDonut data={zero} />)
    expect(screen.getByRole('img', { name: 'Spending by category' })).toBeInTheDocument()
    expect(screen.queryByText('No spending this month.')).not.toBeInTheDocument()
    expect(screen.getByText('Food')).toBeInTheDocument()
    expect(screen.getByText('₹0 · 0%')).toBeInTheDocument()
  })
})
