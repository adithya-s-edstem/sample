package com.expensetracker.repository;

import com.expensetracker.domain.Category;
import com.expensetracker.domain.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable filter predicates for {@link Expense} queries, matching the list/
 * export query parameters in {@code docs/api-contracts.md} (date range,
 * category, amount range).
 *
 * <p>Each factory returns {@code null} when its argument is {@code null}, which
 * Spring Data treats as "no constraint" when specifications are combined with
 * {@link Specification#allOf}. That lets callers pass every optional filter
 * straight through without null-checking each one.
 */
public final class ExpenseSpecifications {

    private ExpenseSpecifications() {}

    /** {@code date >= from} (inclusive). */
    public static Specification<Expense> dateFrom(LocalDate from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("date"), from);
    }

    /** {@code date <= to} (inclusive). */
    public static Specification<Expense> dateTo(LocalDate to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("date"), to);
    }

    /** {@code category = category}. */
    public static Specification<Expense> hasCategory(Category category) {
        return category == null ? null : (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    /** {@code amount >= min} (inclusive). */
    public static Specification<Expense> minAmount(BigDecimal min) {
        return min == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), min);
    }

    /** {@code amount <= max} (inclusive). */
    public static Specification<Expense> maxAmount(BigDecimal max) {
        return max == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), max);
    }
}
