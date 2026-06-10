package com.expensetracker.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TrendQuery}'s range/granularity resolution behind
 * {@code GET /api/summary/trend}: the shared current-month date defaulting plus
 * the contract's granularity default ({@code day} within a single month,
 * {@code month} across longer spans) and explicit-override behavior.
 */
class TrendQueryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 10);

    @Test
    void defaultsRangeToCurrentMonthWhenNoBounds() {
        TrendQuery query = new TrendQuery(null, null, null);

        assertThat(query.resolvedFrom(TODAY)).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(query.resolvedTo(TODAY)).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void completesOneSidedBoundsLikeOtherSummaries() {
        assertThat(new TrendQuery(LocalDate.of(2026, 2, 5), null, null).resolvedTo(TODAY))
                .isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(new TrendQuery(null, LocalDate.of(2026, 3, 20), null).resolvedFrom(TODAY))
                .isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    void defaultsToDayWhenRangeWithinASingleMonth() {
        // No bounds → current month → a single calendar month → day.
        assertThat(new TrendQuery(null, null, null).resolvedGranularity(TODAY)).isEqualTo(Granularity.DAY);
        // Explicit single-month span (both ends in June 2026).
        assertThat(new TrendQuery(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), null).resolvedGranularity(TODAY))
                .isEqualTo(Granularity.DAY);
    }

    @Test
    void defaultsToMonthWhenRangeSpansMultipleMonths() {
        assertThat(new TrendQuery(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), null).resolvedGranularity(TODAY))
                .isEqualTo(Granularity.MONTH);
        // Even a one-day overflow into the next month flips the default to month.
        assertThat(new TrendQuery(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), null).resolvedGranularity(TODAY))
                .isEqualTo(Granularity.MONTH);
    }

    @Test
    void explicitGranularityOverridesTheDefault() {
        // A multi-month span asked for day buckets, and a single month asked for month.
        assertThat(new TrendQuery(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), Granularity.DAY)
                        .resolvedGranularity(TODAY))
                .isEqualTo(Granularity.DAY);
        assertThat(new TrendQuery(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), Granularity.MONTH)
                        .resolvedGranularity(TODAY))
                .isEqualTo(Granularity.MONTH);
    }

    @Test
    void fromParamIsCaseInsensitiveAndNullSafe() {
        assertThat(Granularity.fromParam(null)).isNull();
        assertThat(Granularity.fromParam("day")).isEqualTo(Granularity.DAY);
        assertThat(Granularity.fromParam("MONTH")).isEqualTo(Granularity.MONTH);
    }
}
