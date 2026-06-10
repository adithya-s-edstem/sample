package com.expensetracker.web.dto;

import com.expensetracker.domain.Category;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Expense response body, matching the {@code Expense (response)} object in
 * {@code docs/api-contracts.md}.
 *
 * <p>{@code id} is a UUID string, {@code amount} an exact decimal, {@code date}
 * an ISO {@code YYYY-MM-DD} calendar date, and {@code createdAt}/{@code updatedAt}
 * ISO-8601 instants serialized in UTC. Built from an {@code Expense} via
 * {@link ExpenseMapper#toResponse}.
 */
public record ExpenseResponse(
        UUID id, BigDecimal amount, LocalDate date, Category category, Instant createdAt, Instant updatedAt) {}
