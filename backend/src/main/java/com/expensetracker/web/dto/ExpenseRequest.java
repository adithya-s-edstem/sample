package com.expensetracker.web.dto;

import com.expensetracker.domain.Category;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create/update request body for an expense, matching {@code ExpenseRequest} in
 * {@code docs/api-contracts.md}.
 *
 * <p>Money stays exact decimal ({@link BigDecimal}); {@code date} is an ISO
 * {@code YYYY-MM-DD} calendar date; {@code category} is one of the fixed
 * {@link Category} values. Bean Validation constraints (amount {@code > 0},
 * required fields) are layered on in P2-4 along with the global error handler;
 * this type just fixes the wire shape and feeds {@link ExpenseMapper}.
 */
public record ExpenseRequest(BigDecimal amount, LocalDate date, Category category) {}
