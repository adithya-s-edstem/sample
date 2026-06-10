package com.expensetracker.web.dto;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Resolved parameters for {@code GET /api/summary/trend}, matching the
 * range-defaulting and {@code granularity} rules in {@code docs/api-contracts.md}.
 *
 * <p>The date range resolves exactly like the other summary endpoints (see
 * {@link SummaryQuery}): neither bound → the current calendar month; one bound →
 * completed to that bound's month; both → verbatim. Every trend response echoes
 * the concrete {@code from}/{@code to} it computed over, so resolution always
 * yields a fully-bounded range.
 *
 * <p>{@code granularity} is optional and, when omitted, follows the contract's
 * default: {@code day} when the resolved range stays within a single calendar
 * month, otherwise {@code month} (see {@link #resolvedGranularity}). {@code today}
 * is passed into resolution so the current-month default stays deterministic in
 * tests.
 */
public record TrendQuery(LocalDate from, LocalDate to, Granularity granularity) {

    /** Effective range start (mirrors {@link SummaryQuery#resolvedFrom}). */
    public LocalDate resolvedFrom(LocalDate today) {
        if (from != null) {
            return from;
        }
        if (to != null) {
            return YearMonth.from(to).atDay(1);
        }
        return YearMonth.from(today).atDay(1);
    }

    /** Effective range end (mirrors {@link SummaryQuery#resolvedTo}). */
    public LocalDate resolvedTo(LocalDate today) {
        if (to != null) {
            return to;
        }
        if (from != null) {
            return YearMonth.from(from).atEndOfMonth();
        }
        return YearMonth.from(today).atEndOfMonth();
    }

    /**
     * Effective bucket size: the explicit {@code granularity} when given,
     * otherwise the contract default — {@code day} when the resolved range lies
     * within a single calendar month, {@code month} across any longer span.
     */
    public Granularity resolvedGranularity(LocalDate today) {
        if (granularity != null) {
            return granularity;
        }
        LocalDate effFrom = resolvedFrom(today);
        LocalDate effTo = resolvedTo(today);
        boolean withinOneMonth = YearMonth.from(effFrom).equals(YearMonth.from(effTo));
        return withinOneMonth ? Granularity.DAY : Granularity.MONTH;
    }
}
