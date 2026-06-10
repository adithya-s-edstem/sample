package com.expensetracker.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Spending-over-time series for a period, matching the
 * {@code GET /api/summary/trend} body in {@code docs/api-contracts.md}:
 *
 * <pre>{ from, to, granularity, points: [ { period, total } ] }</pre>
 *
 * <p>{@code from}/{@code to} are the resolved (always concrete) ISO {@code
 * YYYY-MM-DD} bounds the figures were computed over. {@code granularity} is the
 * effective bucket size ({@code day} / {@code month}). {@code points} holds one
 * {@link TrendPoint} per non-empty bucket, in ascending calendar order; an empty
 * period yields an empty list.
 */
public record TrendResponse(LocalDate from, LocalDate to, Granularity granularity, List<TrendPoint> points) {

    /**
     * A single bucket of the series.
     *
     * @param period bucket key — {@code YYYY-MM-DD} for day granularity,
     *     {@code YYYY-MM} for month granularity.
     * @param total exact decimal sum of amounts in the bucket ({@code BigDecimal},
     *     scale 2).
     */
    public record TrendPoint(String period, BigDecimal total) {}
}
