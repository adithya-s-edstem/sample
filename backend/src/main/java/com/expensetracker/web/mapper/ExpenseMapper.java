package com.expensetracker.web.mapper;

import com.expensetracker.domain.Expense;
import com.expensetracker.web.dto.ExpenseRequest;
import com.expensetracker.web.dto.ExpenseResponse;

/**
 * Converts between the {@link Expense} entity and its API DTOs
 * ({@link ExpenseRequest} / {@link ExpenseResponse}).
 *
 * <p>Stateless static helpers, following the {@code ExpenseSpecifications} idiom.
 * The mapper never touches the generated {@code id} or the lifecycle-managed
 * {@code createdAt}/{@code updatedAt} timestamps: those are owned by JPA, so
 * {@link #toEntity} and {@link #applyTo} only carry the client-supplied fields
 * (amount, date, category).
 */
public final class ExpenseMapper {

    private ExpenseMapper() {}

    /** Builds a new {@link Expense} from a create request. */
    public static Expense toEntity(ExpenseRequest request) {
        return new Expense(request.amount(), request.date(), request.category());
    }

    /** Copies the mutable fields of an update request onto an existing entity (PUT). */
    public static void applyTo(ExpenseRequest request, Expense expense) {
        expense.setAmount(request.amount());
        expense.setDate(request.date());
        expense.setCategory(request.category());
    }

    /** Projects an entity to its response DTO. */
    public static ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getDate(),
                expense.getCategory(),
                expense.getCreatedAt(),
                expense.getUpdatedAt());
    }
}
