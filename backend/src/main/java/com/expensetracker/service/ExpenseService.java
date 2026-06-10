package com.expensetracker.service;

import com.expensetracker.domain.Expense;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.ExpenseSpecifications;
import com.expensetracker.repository.projection.CategorySummaryProjection;
import com.expensetracker.repository.projection.SummaryProjection;
import com.expensetracker.web.dto.CategorySummaryResponse;
import com.expensetracker.web.dto.CategorySummaryResponse.CategoryShare;
import com.expensetracker.web.dto.ExpenseQuery;
import com.expensetracker.web.dto.ExpenseRequest;
import com.expensetracker.web.dto.ExpenseResponse;
import com.expensetracker.web.dto.PageResponse;
import com.expensetracker.web.dto.SummaryQuery;
import com.expensetracker.web.dto.SummaryResponse;
import com.expensetracker.web.mapper.ExpenseMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD business logic for expenses, sitting between the controller (P2-3) and
 * the repository (P1-4).
 *
 * <p>Accepts/returns DTOs so the controller stays thin; entities never leak past
 * this layer. Reads/updates of a missing id raise {@link ExpenseNotFoundException}
 * (→ 404 via the P2-4 handler). Validation (amount {@code > 0}, required fields)
 * is enforced at the API boundary by Bean Validation (P2-4) and the DB CHECK
 * constraint, so it is not re-checked here.
 */
@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    /** Persists a new expense and returns its response view. */
    public ExpenseResponse create(ExpenseRequest request) {
        Expense saved = repository.save(ExpenseMapper.toEntity(request));
        return ExpenseMapper.toResponse(saved);
    }

    /** Fetches one expense or throws {@link ExpenseNotFoundException}. */
    @Transactional(readOnly = true)
    public ExpenseResponse get(UUID id) {
        return ExpenseMapper.toResponse(findOrThrow(id));
    }

    /**
     * Filtered, sorted, paginated list of expenses ({@code GET /api/expenses}).
     *
     * <p>Applies the contract's defaulting via {@link ExpenseQuery}: when neither
     * {@code from} nor {@code to} is supplied the range is the current calendar
     * month; sort defaults to {@code date,desc}; page/size default to {@code 0}/
     * {@code 50} (capped at {@code 200}). Filters compose through
     * {@link ExpenseSpecifications}, so any omitted filter imposes no constraint.
     */
    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(ExpenseQuery query) {
        LocalDate today = LocalDate.now();
        Specification<Expense> spec = Specification.allOf(
                ExpenseSpecifications.dateFrom(query.resolvedFrom(today)),
                ExpenseSpecifications.dateTo(query.resolvedTo(today)),
                ExpenseSpecifications.hasCategory(query.resolvedCategory()),
                ExpenseSpecifications.minAmount(query.minAmount()),
                ExpenseSpecifications.maxAmount(query.maxAmount()));

        Page<ExpenseResponse> page =
                repository.findAll(spec, query.toPageRequest()).map(ExpenseMapper::toResponse);
        return PageResponse.of(page, query.resolvedSort());
    }

    /**
     * Headline summary (total + count) for a period ({@code GET /api/summary}).
     *
     * <p>Resolves the date range via {@link SummaryQuery} (current month when
     * {@code from}/{@code to} are omitted) and aggregates server-side. The total is
     * normalized to the money scale ({@code NUMERIC(12,2)} → scale 2) so an empty
     * period reads {@code 0.00} and the value is exact decimal throughout.
     */
    @Transactional(readOnly = true)
    public SummaryResponse summary(SummaryQuery query) {
        LocalDate today = LocalDate.now();
        LocalDate from = query.resolvedFrom(today);
        LocalDate to = query.resolvedTo(today);

        SummaryProjection projection = repository.summarize(from, to);
        BigDecimal total = projection.getTotal().setScale(2, RoundingMode.UNNECESSARY);
        return SummaryResponse.of(from, to, total, projection.getCount());
    }

    /**
     * Per-category breakdown (total, count, percent share) for a period
     * ({@code GET /api/summary/by-category}).
     *
     * <p>Resolves the date range via {@link SummaryQuery} (current month when
     * {@code from}/{@code to} are omitted) and aggregates server-side, one row per
     * category that has spend in range (largest-first; empty categories omitted).
     * The grand {@code total} and each slice {@code total} are normalized to the
     * money scale (scale 2). Each {@code percent} is the slice's share of the grand
     * total, computed from the exact {@code BigDecimal} totals and rounded
     * half-up to two decimals; an empty period yields {@code total = 0.00} and an
     * empty list (no division by zero).
     */
    @Transactional(readOnly = true)
    public CategorySummaryResponse summaryByCategory(SummaryQuery query) {
        LocalDate today = LocalDate.now();
        LocalDate from = query.resolvedFrom(today);
        LocalDate to = query.resolvedTo(today);

        List<CategorySummaryProjection> rows = repository.summarizeByCategory(from, to);
        BigDecimal total = rows.stream()
                .map(CategorySummaryProjection::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);

        List<CategoryShare> categories = rows.stream()
                .map(row -> new CategoryShare(
                        row.getCategory(),
                        row.getTotal().setScale(2, RoundingMode.UNNECESSARY),
                        row.getCount(),
                        percentOf(row.getTotal(), total)))
                .toList();

        return new CategorySummaryResponse(from, to, total, categories);
    }

    /**
     * A category total's share of the grand total, as a percentage rounded half-up
     * to two decimals. Returns {@code 0.00} when the grand total is zero (only
     * possible for an empty period, where there are no slices anyway), avoiding a
     * divide-by-zero.
     */
    private static BigDecimal percentOf(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    /** Full-replace update of an existing expense; throws if the id is unknown. */
    public ExpenseResponse update(UUID id, ExpenseRequest request) {
        Expense expense = findOrThrow(id);
        ExpenseMapper.applyTo(request, expense);
        return ExpenseMapper.toResponse(repository.save(expense));
    }

    /** Deletes an existing expense; throws if the id is unknown. */
    public void delete(UUID id) {
        repository.delete(findOrThrow(id));
    }

    private Expense findOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ExpenseNotFoundException(id));
    }
}
