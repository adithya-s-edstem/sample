import { describe, it, expect } from 'vitest'
import { expenseCountLabel, formatINR, formatINRExact } from './money'

describe('formatINR', () => {
  it('formats whole rupee amounts with Indian grouping and no decimals', () => {
    expect(formatINR(24500)).toBe('₹24,500')
    expect(formatINR(0)).toBe('₹0')
  })

  it('uses lakh/crore grouping for large amounts', () => {
    expect(formatINR(1000)).toBe('₹1,000')
    expect(formatINR(100000)).toBe('₹1,00,000')
    expect(formatINR(10000000)).toBe('₹1,00,00,000')
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

describe('formatINRExact', () => {
  it('always shows two decimals, including for whole amounts (table column)', () => {
    expect(formatINRExact(1200)).toBe('₹1,200.00')
    expect(formatINRExact(300)).toBe('₹300.00')
    expect(formatINRExact(0)).toBe('₹0.00')
  })

  it('formats paise exactly with Indian grouping', () => {
    expect(formatINRExact(1234.56)).toBe('₹1,234.56')
    expect(formatINRExact(1234.5)).toBe('₹1,234.50')
    expect(formatINRExact(0.01)).toBe('₹0.01')
    expect(formatINRExact(1200000)).toBe('₹12,00,000.00')
  })

  it('keeps lakh/crore grouping on paise-bearing large amounts (no scientific notation)', () => {
    expect(formatINRExact(9999999.99)).toBe('₹99,99,999.99')
    const formatted = formatINRExact(123456789.5)
    expect(formatted).toBe('₹12,34,56,789.50')
    expect(formatted).not.toMatch(/[eE]/)
  })
})

describe('money/decimal precision (testing-plan §6, frontend display)', () => {
  // §6.5: INR formatting must match the stored two-decimal wire value with no
  // rounding errors introduced in the UI layer. Amounts arrive as exact
  // two-decimal numbers from the BigDecimal/NUMERIC(12,2) backend; display-only
  // formatting must not drift.
  it('renders the classic float-trap result without drift', () => {
    // 0.1 + 0.2 === 0.30000000000000004 in IEEE-754; the wire value is the
    // exact aggregated 0.3, which must read as ₹0.30 (paise), not ₹0.30000000004.
    expect(formatINRExact(0.3)).toBe('₹0.30')
    expect(formatINR(0.3)).toBe('₹0.30')
  })

  it('renders the smallest positive amount as ₹0.01', () => {
    expect(formatINRExact(0.01)).toBe('₹0.01')
    expect(formatINR(0.01)).toBe('₹0.01')
  })

  it('renders the top of the NUMERIC(12,2) range in full, no scientific notation', () => {
    const formatted = formatINRExact(9999999999.99)
    expect(formatted).toBe('₹9,99,99,99,999.99')
    expect(formatted).not.toMatch(/[eE]/)
  })

  it('preserves paise exactly across a sweep of two-decimal wire values', () => {
    expect(formatINRExact(1.05)).toBe('₹1.05')
    expect(formatINRExact(12.34)).toBe('₹12.34')
    expect(formatINRExact(99.99)).toBe('₹99.99')
    expect(formatINRExact(100.1)).toBe('₹100.10')
  })
})

describe('expenseCountLabel', () => {
  it('pluralizes the count', () => {
    expect(expenseCountLabel(0)).toBe('0 expenses')
    expect(expenseCountLabel(1)).toBe('1 expense')
    expect(expenseCountLabel(42)).toBe('42 expenses')
  })
})
