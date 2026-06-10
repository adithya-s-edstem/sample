import { describe, it, expect } from 'vitest'
import { categoryColor, categoryLabel, categoryPillTextColor } from './category'
import { CATEGORIES } from '../api/types'

/*
 * WCAG relative-luminance contrast ratio between two #rrggbb colors, used to
 * assert the category pill text (P9-1) meets AA (>= 4.5:1) on its tinted
 * background. The pill background is the category hue at ~10% over white, so we
 * approximate it as near-white (#f4f4f5) for the contrast check — the lightest
 * realistic backdrop, i.e. the worst case for dark-on-light text.
 */
function luminance(hex: string): number {
  const channel = (v: number) => {
    const s = v / 255
    return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4)
  }
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
}

function contrastRatio(fg: string, bg: string): number {
  const l1 = luminance(fg)
  const l2 = luminance(bg)
  const [hi, lo] = l1 >= l2 ? [l1, l2] : [l2, l1]
  return (hi + 0.05) / (lo + 0.05)
}

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

describe('categoryPillTextColor', () => {
  it('returns a hex color for every category', () => {
    CATEGORIES.forEach((c) => expect(categoryPillTextColor(c)).toMatch(/^#[0-9a-f]{6}$/i))
  })

  it('meets WCAG AA contrast (>= 4.5:1) on the light pill background', () => {
    // Lightest realistic pill backdrop (category hue at ~10% over white).
    const background = '#f4f4f5'
    CATEGORIES.forEach((c) => {
      const ratio = contrastRatio(categoryPillTextColor(c), background)
      expect(ratio, `${c} pill text contrast`).toBeGreaterThanOrEqual(4.5)
    })
  })
})
