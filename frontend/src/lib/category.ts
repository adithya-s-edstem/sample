/*
 * Category display helpers for the dashboard (P6-2). The backend speaks the fixed
 * UPPERCASE `Category` enum (docs/api-contracts.md, no custom categories in v1);
 * the UI renders Title-case labels and a stable color per category so the donut
 * slices and legend stay consistent across renders and match the wireframe
 * palette (docs/wireframes/dashboard.html + styles.css).
 */
import type { Category } from '../api/types'

/** Title-case label for a category, e.g. `RENT` → `Rent`. */
export function categoryLabel(category: Category): string {
  return category.charAt(0) + category.slice(1).toLowerCase()
}

/*
 * Fixed slice/legend color per category, drawn from the wireframe donut palette.
 * Keyed by category (not by ordinal position) so a category keeps the same color
 * whether it's the largest slice or the smallest, and across month changes.
 */
const CATEGORY_COLORS: Record<Category, string> = {
  RENT: '#4f46e5',
  GROCERIES: '#06b6d4',
  FOOD: '#f59e0b',
  TRANSPORT: '#10b981',
  UTILITIES: '#ef4444',
  SHOPPING: '#a78bfa',
  ENTERTAINMENT: '#ec4899',
  HEALTH: '#14b8a6',
  OTHER: '#9ca3af',
}

/** Stable donut/legend color for a category. */
export function categoryColor(category: Category): string {
  return CATEGORY_COLORS[category]
}

/*
 * Accessibility (P9-1): the category "pill" tints its background with the
 * category color at ~10% and prints the label in that same color. Several of the
 * donut colors (amber, cyan, the violet/teal/pink accents) are too light to meet
 * WCAG AA (4.5:1) as text on a near-white tint, so the pill uses a darkened
 * variant of each color for the text while keeping the recognizable hue. The
 * darker text keeps the pill legible without changing the donut/legend palette.
 */
const CATEGORY_PILL_TEXT: Record<Category, string> = {
  RENT: '#4338ca',
  GROCERIES: '#0e7490',
  FOOD: '#b45309',
  TRANSPORT: '#047857',
  UTILITIES: '#b91c1c',
  SHOPPING: '#6d28d9',
  ENTERTAINMENT: '#be185d',
  HEALTH: '#0f766e',
  OTHER: '#4b5563',
}

/** AA-contrast text color for the category pill (darker than the donut hue). */
export function categoryPillTextColor(category: Category): string {
  return CATEGORY_PILL_TEXT[category]
}
