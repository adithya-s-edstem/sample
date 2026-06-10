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
