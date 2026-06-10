package com.expensetracker.service;

import com.expensetracker.domain.Expense;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.web.dto.ExpenseRequest;
import com.expensetracker.web.dto.ExpenseResponse;
import com.expensetracker.web.mapper.ExpenseMapper;
import java.util.UUID;
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
