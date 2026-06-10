package com.expensetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.expensetracker.TestcontainersConfiguration;
import com.expensetracker.domain.Category;
import com.expensetracker.domain.Expense;
import com.expensetracker.repository.projection.CategorySummaryProjection;
import com.expensetracker.repository.projection.SummaryProjection;
import com.expensetracker.repository.projection.TrendProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Money/decimal precision suite for the server-side aggregation queries (P3-5),
 * driving the JPQL ({@code summarize}, {@code summarizeByCategory}) and native
 * ({@code summarizeTrendByDay} / {@code summarizeTrendByMonth}) queries against a
 * real Flyway-migrated Postgres via Testcontainers.
 *
 * <p>Guards the testing-plan §6 invariant — {@code NUMERIC(12,2)} / {@link
 * BigDecimal} aggregation must never drift through a float. It checks the classic
 * float-trap sums ({@code 0.10 + 0.20 = 0.30}), many-small-amount accumulation,
 * high-value totals near the {@code NUMERIC(12,2)} ceiling, and that every sum
 * comes back at the money scale (2) so it serializes as {@code N.NN}. The
 * percent-share rounding rule lives in the service, so it is covered by the
 * service unit test; here we only pin that the raw category/grand totals the
 * service divides are themselves exact.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class SummaryPrecisionIntegrationTest {

    // A single month so the whole suite shares one inclusive range.
    private static final LocalDate FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate TO = LocalDate.of(2026, 6, 30);

    @Autowired
    private ExpenseRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
        repository.flush();
    }

    private void save(String amount, LocalDate date, Category category) {
        repository.save(new Expense(new BigDecimal(amount), date, category));
    }

    @Test
    void summarizeAddsTheClassicFloatTrapExactly() {
        // 0.10 + 0.20 = 0.30 — the canonical IEEE-754 drift case (0.30000000000004).
        save("0.10", LocalDate.of(2026, 6, 5), Category.FOOD);
        save("0.20", LocalDate.of(2026, 6, 6), Category.FOOD);
        repository.flush();

        SummaryProjection summary = repository.summarize(FROM, TO);

        assertThat(summary.getTotal()).isEqualByComparingTo("0.30");
        // Exact equality (not just compareTo) — a binary float would land elsewhere.
        assertThat(summary.getTotal()).isEqualTo(new BigDecimal("0.30"));
        assertThat(summary.getCount()).isEqualTo(2L);
    }

    @Test
    void summarizeAccumulatesManySmallAmountsWithoutDrift() {
        // 100 rows of 0.01 must total exactly 1.00 — small amounts are where float
        // accumulation error compounds fastest.
        for (int i = 0; i < 100; i++) {
            save("0.01", LocalDate.of(2026, 6, 10), Category.OTHER);
        }
        repository.flush();

        SummaryProjection summary = repository.summarize(FROM, TO);

        assertThat(summary.getTotal()).isEqualByComparingTo("1.00");
        assertThat(summary.getCount()).isEqualTo(100L);
    }

    @Test
    void summarizeHandlesHighValueTotalsNearTheNumericCeiling() {
        // NUMERIC(12,2) holds up to 9_999_999_999.99. Two large rows must sum
        // exactly without overflow or rounding.
        save("9999999.99", LocalDate.of(2026, 6, 1), Category.RENT);
        save("0.01", LocalDate.of(2026, 6, 2), Category.RENT);
        repository.flush();

        SummaryProjection summary = repository.summarize(FROM, TO);

        assertThat(summary.getTotal()).isEqualByComparingTo("10000000.00");
        assertThat(summary.getCount()).isEqualTo(2L);
    }

    @Test
    void summarizeByCategoryTotalsAreExactAndReconcileToTheGrandTotal() {
        // Mixed categories with fractional amounts; the per-category sums and their
        // reduction must reconcile exactly to the headline summarize() total.
        save("10.10", LocalDate.of(2026, 6, 1), Category.FOOD);
        save("20.20", LocalDate.of(2026, 6, 2), Category.FOOD);
        save("0.05", LocalDate.of(2026, 6, 3), Category.TRANSPORT);
        save("0.05", LocalDate.of(2026, 6, 4), Category.TRANSPORT);
        save("33.33", LocalDate.of(2026, 6, 5), Category.GROCERIES);
        repository.flush();

        List<CategorySummaryProjection> rows = repository.summarizeByCategory(FROM, TO);

        BigDecimal byCategory =
                rows.stream().map(CategorySummaryProjection::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(byCategory).isEqualByComparingTo("63.73");

        // FOOD: 10.10 + 20.20 = 30.30; TRANSPORT: 0.05 + 0.05 = 0.10.
        BigDecimal food = totalFor(rows, Category.FOOD);
        BigDecimal transport = totalFor(rows, Category.TRANSPORT);
        assertThat(food).isEqualByComparingTo("30.30");
        assertThat(transport).isEqualByComparingTo("0.10");

        // The grouped breakdown must reconcile to the un-grouped headline total.
        assertThat(byCategory)
                .isEqualByComparingTo(repository.summarize(FROM, TO).getTotal());
    }

    @Test
    void summarizeTrendByDaySumsSameDayFractionalRowsExactly() {
        // Three fractional rows on one day → one bucket summing exactly (0.30),
        // through the native to_char query.
        save("0.10", LocalDate.of(2026, 6, 15), Category.FOOD);
        save("0.10", LocalDate.of(2026, 6, 15), Category.FOOD);
        save("0.10", LocalDate.of(2026, 6, 15), Category.FOOD);
        repository.flush();

        List<TrendProjection> days = repository.summarizeTrendByDay(FROM, TO);

        assertThat(days).hasSize(1);
        assertThat(days.get(0).getPeriod()).isEqualTo("2026-06-15");
        assertThat(days.get(0).getTotal()).isEqualByComparingTo("0.30");
    }

    @Test
    void summarizeTrendByMonthAccumulatesManySmallAmountsWithoutDrift() {
        // 100 rows of 0.01 across June collapse into one month bucket totalling 1.00.
        for (int i = 0; i < 100; i++) {
            save("0.01", LocalDate.of(2026, 6, 12), Category.UTILITIES);
        }
        repository.flush();

        List<TrendProjection> months = repository.summarizeTrendByMonth(FROM, TO);

        assertThat(months).hasSize(1);
        assertThat(months.get(0).getPeriod()).isEqualTo("2026-06");
        assertThat(months.get(0).getTotal()).isEqualByComparingTo("1.00");
    }

    @Test
    void aggregationTotalsRoundTripWithoutLosingScale() {
        // A whole-rupee amount stored as NUMERIC(12,2) must aggregate back at scale
        // 2 (1234.00, not 1234), so the wire renders N.NN across every endpoint.
        save("1234.00", LocalDate.of(2026, 6, 20), Category.SHOPPING);
        repository.flush();

        assertThat(repository.summarize(FROM, TO).getTotal().scale()).isEqualTo(2);
        assertThat(repository.summarizeByCategory(FROM, TO).get(0).getTotal().scale())
                .isEqualTo(2);
        assertThat(repository.summarizeTrendByDay(FROM, TO).get(0).getTotal().scale())
                .isEqualTo(2);
    }

    private static BigDecimal totalFor(List<CategorySummaryProjection> rows, Category category) {
        return rows.stream()
                .filter(r -> r.getCategory() == category)
                .map(CategorySummaryProjection::getTotal)
                .findFirst()
                .orElseThrow();
    }
}
