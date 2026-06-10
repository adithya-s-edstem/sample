package com.expensetracker.service;

import com.expensetracker.domain.Expense;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.ExpenseSpecifications;
import com.expensetracker.repository.projection.SummaryProjection;
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
