package com.expensetracker.repository.projection;

import java.math.BigDecimal;

/**
 * Spring Data projection for one bucket of the time-series aggregation behind
 * {@code GET /api/summary/trend} (P3-4), produced by
 * {@link com.expensetracker.repository.ExpenseRepository#summarizeTrendByDay} and
 * {@link com.expensetracker.repository.ExpenseRepository#summarizeTrendByMonth}.
 *
 * <p>One row per non-empty period in range. {@code period} is the bucket key,
 * already formatted in the database to the contract's calendar shape —
 * {@code YYYY-MM-DD} for day granularity, {@code YYYY-MM} for month granularity —
 * so the response can echo it verbatim. {@code total} stays {@link BigDecimal}
 * (never a float) so money precision is preserved from the database through to
 * the response.
 */
public interface TrendProjection {

    String getPeriod();

    BigDecimal getTotal();
}
