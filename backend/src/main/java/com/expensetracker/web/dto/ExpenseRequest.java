package com.expensetracker.web.dto;

import com.expensetracker.domain.Category;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create/update request body for an expense, matching {@code ExpenseRequest} in
 * {@code docs/api-contracts.md}.
 *
 * <p>Money stays exact decimal ({@link BigDecimal}); {@code date} is an ISO
 * {@code YYYY-MM-DD} calendar date; {@code category} is one of the fixed
 * {@link Category} values. Bean Validation enforces the contract's rules
 * (amount required and {@code > 0}; date and category required) at the API
 * boundary; the global handler renders any violation as the uniform 400 error
 * shape. A malformed date or an unknown category fails JSON binding and is also
 * surfaced as a 400.
 */
public record ExpenseRequest(
        @NotNull @Positive BigDecimal amount, @NotNull LocalDate date, @NotNull Category category) {}
