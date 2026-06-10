package com.expensetracker.repository;

import com.expensetracker.domain.Expense;
import com.expensetracker.repository.projection.SummaryProjection;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence for {@link Expense}.
 *
 * <p>CRUD, sorting, and pagination come from {@link JpaRepository}. The list/
 * export endpoints (P3-1, P4-1) need an open-ended combination of optional
 * filters (date range, category, amount range), so we expose
 * {@link JpaSpecificationExecutor} and compose those filters with
 * {@link ExpenseSpecifications} rather than enumerating a derived-query method
 * per filter combination.
 *
 * <p>The summary endpoints (P3-2…) need server-side aggregation rather than
 * fetching rows; those run as dedicated JPQL queries so the sum/count are
 * computed in Postgres and money precision is preserved end to end.
 */
public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

    /**
     * Total amount and expense count over an inclusive date range, computed in the
     * database ({@code GET /api/summary}, P3-2).
     *
     * <p>{@code SUM} returns {@code null} for an empty range; the service coalesces
     * that to {@code 0.00}. {@code total} stays {@code BigDecimal} so no float ever
     * touches the money value.
     */
    @Query(
            """
            select coalesce(sum(e.amount), 0) as total, count(e) as count
            from Expense e
            where e.date >= :from and e.date <= :to
            """)
    SummaryProjection summarize(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
