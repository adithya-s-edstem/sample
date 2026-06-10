package com.expensetracker.domain;

/**
 * The fixed set of expense categories supported in v1. Custom categories are
 * explicitly out of scope, so this enum is the single source of truth: it is
 * persisted by name ({@code @Enumerated(EnumType.STRING)}), surfaced verbatim
 * over the API (e.g. {@code "GROCERIES"}), and exposed by the
 * {@code GET /api/categories} reference endpoint.
 *
 * <p>The declaration order is the canonical display/API order and is relied on
 * by tests — keep it aligned with {@code docs/api-contracts.md}.
 */
public enum Category {
    FOOD,
    TRANSPORT,
    RENT,
    UTILITIES,
    GROCERIES,
    ENTERTAINMENT,
    HEALTH,
    SHOPPING,
    OTHER
}
