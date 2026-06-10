package com.expensetracker.web.dto;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Resolved date-range parameters shared by the summary endpoints
 * ({@code GET /api/summary} and, later, {@code /summary/by-category} and
 * {@code /summary/trend}), matching the range-defaulting rule in
 * {@code docs/api-contracts.md}.
 *
 * <p>Unlike {@link ExpenseQuery} — whose list response never echoes the range and
 * so can leave a one-sided bound {@code null} — every summary response echoes the
 * concrete {@code from}/{@code to} it computed over. The resolution here therefore
 * always yields a fully-bounded range:
 *
 * <ul>
 *   <li>neither bound given → the current calendar month (first day … last day);
 *   <li>only {@code from} given → {@code from} … last day of {@code from}'s month;
 *   <li>only {@code to} given → first day of {@code to}'s month … {@code to};
 *   <li>both given → used verbatim.
 * </ul>
 *
 * <p>{@code today} is passed into the resolution so the current-month default stays
 * deterministic in tests.
 */
public record SummaryQuery(LocalDate from, LocalDate to) {

    /** Effective range start. */
    public LocalDate resolvedFrom(LocalDate today) {
        if (from != null) {
            return from;
        }
        if (to != null) {
            return YearMonth.from(to).atDay(1);
        }
        return YearMonth.from(today).atDay(1);
    }

    /** Effective range end. */
    public LocalDate resolvedTo(LocalDate today) {
        if (to != null) {
            return to;
        }
        if (from != null) {
            return YearMonth.from(from).atEndOfMonth();
        }
        return YearMonth.from(today).atEndOfMonth();
    }
}
