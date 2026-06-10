package com.expensetracker.repository.projection;

import java.math.BigDecimal;

/**
 * Spring Data projection for the aggregated {@code GET /api/summary} query
 * ({@link com.expensetracker.repository.ExpenseRepository#summarize}).
 *
 * <p>Keeps the total as {@link BigDecimal} (never a float) so money precision is
 * preserved from the database through to the response. {@code getTotal()} is
 * coalesced to {@code 0} in the query, so it is never {@code null}.
 */
public interface SummaryProjection {

    BigDecimal getTotal();

    long getCount();
}
