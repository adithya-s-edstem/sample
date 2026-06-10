package com.expensetracker.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SummaryQuery}'s date-range resolution (the current-month
 * defaulting and one-sided-bound completion behind {@code GET /api/summary}).
 */
class SummaryQueryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 10);

    @Test
    void defaultsToCurrentMonthWhenNoBounds() {
        SummaryQuery query = new SummaryQuery(null, null);

        assertThat(query.resolvedFrom(TODAY)).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(query.resolvedTo(TODAY)).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void completesToEndOfFromsMonthWhenOnlyFromGiven() {
        SummaryQuery query = new SummaryQuery(LocalDate.of(2026, 2, 5), null);

        assertThat(query.resolvedFrom(TODAY)).isEqualTo(LocalDate.of(2026, 2, 5));
        // February 2026 ends on the 28th (non-leap year).
        assertThat(query.resolvedTo(TODAY)).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void completesToStartOfTosMonthWhenOnlyToGiven() {
        SummaryQuery query = new SummaryQuery(null, LocalDate.of(2026, 3, 20));

        assertThat(query.resolvedFrom(TODAY)).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(query.resolvedTo(TODAY)).isEqualTo(LocalDate.of(2026, 3, 20));
    }

    @Test
    void usesBothBoundsVerbatimWhenGiven() {
        SummaryQuery query = new SummaryQuery(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(query.resolvedFrom(TODAY)).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(query.resolvedTo(TODAY)).isEqualTo(LocalDate.of(2026, 12, 31));
    }
}
