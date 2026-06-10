import { describe, it, expect } from 'vitest'
import { categoryColor, categoryLabel } from './category'
import { CATEGORIES } from '../api/types'

describe('categoryLabel', () => {
  it('Title-cases the fixed enum values', () => {
    expect(categoryLabel('RENT')).toBe('Rent')
    expect(categoryLabel('GROCERIES')).toBe('Groceries')
    expect(categoryLabel('ENTERTAINMENT')).toBe('Entertainment')
    expect(categoryLabel('OTHER')).toBe('Other')
  })
})

describe('categoryColor', () => {
  it('returns a distinct hex color for every category', () => {
    const colors = CATEGORIES.map(categoryColor)
    colors.forEach((c) => expect(c).toMatch(/^#[0-9a-f]{6}$/i))
    expect(new Set(colors).size).toBe(CATEGORIES.length)
  })

  it('is stable for a given category (keyed by category, not position)', () => {
    expect(categoryColor('RENT')).toBe(categoryColor('RENT'))
  })
})
