import { describe, it, expect } from 'vitest'
import { expenseCountLabel, formatINR } from './money'

describe('formatINR', () => {
  it('formats whole rupee amounts with Indian grouping and no decimals', () => {
    expect(formatINR(24500)).toBe('₹24,500')
    expect(formatINR(0)).toBe('₹0')
  })

  it('uses lakh/crore grouping for large amounts', () => {
    expect(formatINR(100000)).toBe('₹1,00,000')
    expect(formatINR(12345678)).toBe('₹1,23,45,678')
  })

  it('shows two decimals when paise are present (exact, no float drift)', () => {
    expect(formatINR(1234.56)).toBe('₹1,234.56')
    // a single-decimal value still reads as two-decimal paise
    expect(formatINR(1234.5)).toBe('₹1,234.50')
    // smallest representable amount
    expect(formatINR(0.01)).toBe('₹0.01')
  })
})

describe('expenseCountLabel', () => {
  it('pluralizes the count', () => {
    expect(expenseCountLabel(0)).toBe('0 expenses')
    expect(expenseCountLabel(1)).toBe('1 expense')
    expect(expenseCountLabel(42)).toBe('42 expenses')
  })
})
