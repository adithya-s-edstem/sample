package com.expensetracker.web.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expensetracker.domain.Category;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Unit tests for {@link ExpenseQuery} — the resolution rules behind
 * {@code GET /api/expenses}: current-month date defaulting, the {@code q}→category
 * mapping, sort parsing/whitelisting, and page/size defaults with the size cap.
 */
class ExpenseQueryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 10);

    private static ExpenseQuery query(LocalDate from, LocalDate to) {
        return new ExpenseQuery(from, to, null, null, null, null, null, null, null);
    }

    // --- date-range defaulting ----------------------------------------------

    @Test
    void defaultsToCurrentMonthWhenBothBoundsOmitted() {
        ExpenseQuery q = query(null, null);

        assertThat(q.resolvedFrom(TODAY)).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(q.resolvedTo(TODAY)).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void honorsExplicitBoundsVerbatim() {
        ExpenseQuery q = query(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

        assertThat(q.resolvedFrom(TODAY)).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(q.resolvedTo(TODAY)).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void leavesOppositeBoundOpenWhenOnlyOneSupplied() {
        // Only `from` → no implicit `to` (open-ended), and vice versa, so a single
        // bound doesn't silently clamp to the current month.
        assertThat(query(LocalDate.of(2026, 1, 1), null).resolvedTo(TODAY)).isNull();
        assertThat(query(null, LocalDate.of(2026, 12, 31)).resolvedFrom(TODAY)).isNull();
    }

    // --- q → category mapping -----------------------------------------------

    @Test
    void explicitCategoryWins() {
        ExpenseQuery q = new ExpenseQuery(null, null, Category.RENT, null, null, "food", null, null, null);

        assertThat(q.resolvedCategory()).isEqualTo(Category.RENT);
    }

    @Test
    void qResolvesToCategoryCaseInsensitively() {
        ExpenseQuery q = new ExpenseQuery(null, null, null, null, null, "  Groceries ", null, null, null);

        assertThat(q.resolvedCategory()).isEqualTo(Category.GROCERIES);
    }

    @Test
    void qThatIsNotACategoryImposesNoConstraint() {
        ExpenseQuery q = new ExpenseQuery(null, null, null, null, null, "coffee", null, null, null);

        assertThat(q.resolvedCategory()).isNull();
    }

    // --- sort ----------------------------------------------------------------

    @Test
    void defaultSortIsDateDesc() {
        assertThat(query(null, null).resolvedSort()).isEqualTo("date,desc");
        assertThat(query(null, null).toPageRequest().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "date"));
    }

    @Test
    void parsesWhitelistedSortFieldAndDirection() {
        ExpenseQuery q = new ExpenseQuery(null, null, null, null, null, null, "amount,asc", null, null);

        assertThat(q.toPageRequest().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "amount"));
    }

    @Test
    void rejectsUnknownSortField() {
        ExpenseQuery q = new ExpenseQuery(null, null, null, null, null, null, "bogus,asc", null, null);

        assertThatThrownBy(q::toPageRequest)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bogus");
    }

    @Test
    void rejectsUnknownSortDirection() {
        ExpenseQuery q = new ExpenseQuery(null, null, null, null, null, null, "date,sideways", null, null);

        assertThatThrownBy(q::toPageRequest).isInstanceOf(IllegalArgumentException.class);
    }

    // --- page / size ---------------------------------------------------------

    @Test
    void defaultsPageZeroSizeFifty() {
        PageRequest pr = query(null, null).toPageRequest();

        assertThat(pr.getPageNumber()).isZero();
        assertThat(pr.getPageSize()).isEqualTo(50);
    }

    @Test
    void capsSizeAtMax() {
        ExpenseQuery q = new ExpenseQuery(null, null, null, null, null, null, null, 0, 5000);

        assertThat(q.toPageRequest().getPageSize()).isEqualTo(200);
    }

    @Test
    void rejectsNegativePage() {
        ExpenseQuery q = new ExpenseQuery(null, null, null, null, null, null, null, -1, null);

        assertThatThrownBy(q::toPageRequest).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveSize() {
        ExpenseQuery q = new ExpenseQuery(null, null, null, null, null, null, null, 0, 0);

        assertThatThrownBy(q::toPageRequest).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void honorsExplicitAmountBoundsCarriedThrough() {
        ExpenseQuery q =
                new ExpenseQuery(null, null, null, new BigDecimal("10.00"), new BigDecimal("99.99"), null, null, 2, 25);

        assertThat(q.minAmount()).isEqualByComparingTo("10.00");
        assertThat(q.maxAmount()).isEqualByComparingTo("99.99");
        assertThat(q.toPageRequest().getPageNumber()).isEqualTo(2);
        assertThat(q.toPageRequest().getPageSize()).isEqualTo(25);
    }
}
