package com.expensetracker.repository;

import com.expensetracker.domain.Expense;
import com.expensetracker.repository.projection.CategorySummaryProjection;
import com.expensetracker.repository.projection.SummaryProjection;
import com.expensetracker.repository.projection.TrendProjection;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.domain.Specification;
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
     * Cursor-backed stream of the matching expenses for CSV export
     * ({@code GET /api/expenses/export}, P4-2). Applies the same dynamic filters and
     * ordering as the list ({@link ExpenseSpecifications} + the resolved {@link
     * org.springframework.data.domain.Sort}) but <b>without pagination</b>, fetching
     * rows lazily through a JDBC cursor instead of materializing the whole result in
     * memory — so an arbitrarily large export streams to the wire at constant memory.
     *
     * <p>The returned {@link Stream} is backed by an open {@code ResultSet}/{@code
     * EntityManager}, so it <b>must</b> be consumed inside the same read-only
     * transaction and closed (try-with-resources). Callers should not buffer it.
     */
    default Stream<Expense> streamAll(Specification<Expense> spec, org.springframework.data.domain.Sort sort) {
        return findBy(spec, query -> query.sortBy(sort).stream());
    }

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

    /**
     * Per-category total and expense count over an inclusive date range, grouped
     * and computed in the database ({@code GET /api/summary/by-category}, P3-3).
     *
     * <p>Returns one row per category that has at least one expense in range
     * (empty categories are omitted). {@code total} stays {@code BigDecimal} so no
     * float ever touches the money value; the percent share is derived in the
     * service from the exact grand total. Ordered by descending total so the donut
     * legend reads largest-first (matching the contract example); ties break on
     * category name for a stable order.
     */
    @Query(
            """
            select e.category as category, sum(e.amount) as total, count(e) as count
            from Expense e
            where e.date >= :from and e.date <= :to
            group by e.category
            order by sum(e.amount) desc, e.category asc
            """)
    List<CategorySummaryProjection> summarizeByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Daily spending series over an inclusive date range, grouped and computed in
     * the database for the {@code day} granularity of {@code GET /api/summary/trend}
     * (P3-4).
     *
     * <p>Native Postgres so the bucket key is formatted to the contract's calendar
     * shape ({@code YYYY-MM-DD}) in the DB; one row per day that has at least one
     * expense (empty days are omitted), ascending by date. {@code total} stays
     * exact decimal (the column is {@code NUMERIC(12,2)}), so no float ever touches
     * the money value.
     */
    @Query(
            value =
                    """
                    select to_char(e.date, 'YYYY-MM-DD') as period, sum(e.amount) as total
                    from expenses e
                    where e.date >= :from and e.date <= :to
                    group by e.date
                    order by e.date asc
                    """,
            nativeQuery = true)
    List<TrendProjection> summarizeTrendByDay(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Monthly spending series over an inclusive date range, grouped and computed in
     * the database for the {@code month} granularity of
     * {@code GET /api/summary/trend} (P3-4).
     *
     * <p>Native Postgres so the bucket key is formatted to the contract's calendar
     * shape ({@code YYYY-MM}) in the DB; one row per month that has at least one
     * expense (empty months are omitted), ascending by month. {@code total} stays
     * exact decimal (the column is {@code NUMERIC(12,2)}), so no float ever touches
     * the money value.
     */
    @Query(
            value =
                    """
                    select to_char(e.date, 'YYYY-MM') as period, sum(e.amount) as total
                    from expenses e
                    where e.date >= :from and e.date <= :to
                    group by to_char(e.date, 'YYYY-MM')
                    order by to_char(e.date, 'YYYY-MM') asc
                    """,
            nativeQuery = true)
    List<TrendProjection> summarizeTrendByMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
