package com.expensetracker.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Headline summary for a period, matching the {@code GET /api/summary} body in
 * {@code docs/api-contracts.md}:
 *
 * <pre>{ from, to, total, count, currency }</pre>
 *
 * <p>{@code from}/{@code to} are the resolved (always concrete) ISO {@code
 * YYYY-MM-DD} bounds the figures were computed over. {@code total} is the exact
 * decimal sum of matching amounts ({@code BigDecimal}, scale 2, {@code 0.00} when
 * the period is empty) and {@code count} the number of matching expenses.
 * {@code currency} is fixed to {@code INR} for v1.
 */
public record SummaryResponse(LocalDate from, LocalDate to, BigDecimal total, long count, String currency) {

    /** v1 is single-currency. */
    public static final String CURRENCY = "INR";

    /** Builds a response over {@code [from, to]} with the fixed v1 currency. */
    public static SummaryResponse of(LocalDate from, LocalDate to, BigDecimal total, long count) {
        return new SummaryResponse(from, to, total, count, CURRENCY);
    }
}
