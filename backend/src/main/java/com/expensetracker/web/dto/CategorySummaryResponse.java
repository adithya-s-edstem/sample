package com.expensetracker.web.dto;

import com.expensetracker.domain.Category;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Per-category breakdown for a period, matching the
 * {@code GET /api/summary/by-category} body in {@code docs/api-contracts.md}:
 *
 * <pre>{ from, to, total, categories: [ { category, total, count, percent } ] }</pre>
 *
 * <p>{@code from}/{@code to} are the resolved (always concrete) ISO {@code
 * YYYY-MM-DD} bounds the figures were computed over. {@code total} is the exact
 * decimal grand total across all categories ({@code BigDecimal}, scale 2,
 * {@code 0.00} when the period is empty). {@code categories} holds one
 * {@link CategoryShare} per category that has spend in range, largest-first; an
 * empty period yields an empty list.
 *
 * @param percent each category's share of {@code total}, as a percentage rounded
 *     to two decimals; computed server-side from the exact totals so the frontend
 *     can render the donut directly.
 */
public record CategorySummaryResponse(LocalDate from, LocalDate to, BigDecimal total, List<CategoryShare> categories) {

    /** A single slice of the breakdown. */
    public record CategoryShare(Category category, BigDecimal total, long count, BigDecimal percent) {}
}
