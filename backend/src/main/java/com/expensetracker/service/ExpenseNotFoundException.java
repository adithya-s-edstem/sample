package com.expensetracker.service;

import java.util.UUID;

/**
 * Thrown when a CRUD operation targets an expense id that does not exist.
 *
 * <p>Maps to HTTP 404 via the global exception handler (P2-4); the service layer
 * stays HTTP-agnostic and just signals "no such expense".
 */
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(UUID id) {
        super("Expense not found: " + id);
    }
}
