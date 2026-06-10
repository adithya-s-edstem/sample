package com.expensetracker.repository.projection;

import com.expensetracker.domain.Category;
import java.math.BigDecimal;

/**
 * Spring Data projection for one row of the per-category aggregation behind
 * {@code GET /api/summary/by-category} (P3-3), produced by
 * {@link com.expensetracker.repository.ExpenseRepository#summarizeByCategory}.
 *
 * <p>One row per category that has at least one expense in range. {@code total}
 * stays {@link BigDecimal} (never a float) so money precision is preserved from
 * the database through to the response; {@code count} is the number of expenses
 * in that category. The percent share is derived server-side in the service from
 * the exact grand total, so it is not part of this read projection.
 */
public interface CategorySummaryProjection {

    Category getCategory();

    BigDecimal getTotal();

    long getCount();
}
