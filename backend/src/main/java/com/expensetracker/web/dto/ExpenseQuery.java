package com.expensetracker.web.dto;

import com.expensetracker.domain.Category;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Resolved query parameters for {@code GET /api/expenses} (and, later, the export
 * endpoint), matching the list contract in {@code docs/api-contracts.md}.
 *
 * <p>Each field is the raw, already-bound optional value from the request. The
 * resolution helpers apply the contract's defaulting rules so the service never
 * repeats them:
 *
 * <ul>
 *   <li><b>Date range</b> — {@code from}/{@code to} default to the current month's
 *       first/last day when omitted (computed against {@code today}, passed in so
 *       it stays testable). If only one bound is given, only that bound applies.
 *   <li><b>Sort</b> — {@code "field,dir"}; defaults to {@code date,desc}. Only the
 *       whitelisted sortable fields are honored; an unknown field or direction is
 *       a {@link IllegalArgumentException} (rendered 400 by the global handler).
 *   <li><b>Page/size</b> — {@code page} defaults to {@code 0}, {@code size} to
 *       {@code 50} and is capped at {@code 200}; negative values are rejected.
 * </ul>
 *
 * <p>{@code q} is reserved free-text search; per the contract it currently matches
 * a category name when it resolves to one, otherwise it imposes no constraint.
 */
public record ExpenseQuery(
        LocalDate from,
        LocalDate to,
        Category category,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String q,
        String sort,
        Integer page,
        Integer size) {

    /** Default sort applied when {@code sort} is omitted. */
    public static final String DEFAULT_SORT = "date,desc";

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    /** Entity fields the list endpoint allows sorting on. */
    private static final Set<String> SORTABLE = Set.of("date", "amount", "category", "createdAt");

    /** Effective range start: explicit {@code from}, else the first day of {@code today}'s month. */
    public LocalDate resolvedFrom(LocalDate today) {
        if (from != null) {
            return from;
        }
        return to != null ? null : YearMonth.from(today).atDay(1);
    }

    /** Effective range end: explicit {@code to}, else the last day of {@code today}'s month. */
    public LocalDate resolvedTo(LocalDate today) {
        if (to != null) {
            return to;
        }
        return from != null ? null : YearMonth.from(today).atEndOfMonth();
    }

    /**
     * The category constraint to apply: an explicit {@code category}, otherwise the
     * category named by {@code q} (reserved free-text search currently maps to a
     * category). Returns {@code null} when neither resolves — i.e. no constraint.
     */
    public Category resolvedCategory() {
        if (category != null) {
            return category;
        }
        if (q == null || q.isBlank()) {
            return null;
        }
        for (Category c : Category.values()) {
            if (c.name().equalsIgnoreCase(q.trim())) {
                return c;
            }
        }
        return null;
    }

    /** The resolved {@code sort} string echoed back in the response. */
    public String resolvedSort() {
        return (sort == null || sort.isBlank()) ? DEFAULT_SORT : sort.trim();
    }

    /**
     * Builds just the {@link Sort} from the resolved sort string, ignoring
     * page/size. Used by the CSV export ({@code GET /api/expenses/export}, P4-1),
     * which applies the same filters and ordering as the list but never paginates.
     */
    public Sort toSort() {
        return parseSort(resolvedSort());
    }

    /** Builds the {@link PageRequest} from page/size/sort, applying defaults and the size cap. */
    public PageRequest toPageRequest() {
        int pageIndex = page == null ? 0 : page;
        if (pageIndex < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        int pageSize = size == null ? DEFAULT_SIZE : size;
        if (pageSize < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        pageSize = Math.min(pageSize, MAX_SIZE);
        return PageRequest.of(pageIndex, pageSize, parseSort(resolvedSort()));
    }

    /** Parses a {@code "field,dir"} sort string against the sortable-field whitelist. */
    private static Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (!SORTABLE.contains(field)) {
            throw new IllegalArgumentException("Invalid sort field '" + field + "'");
        }
        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1 && !parts[1].isBlank()) {
            direction = parseDirection(parts[1].trim());
        }
        return Sort.by(direction, field);
    }

    private static Sort.Direction parseDirection(String dir) {
        List<String> allowed = List.of("asc", "desc");
        if (!allowed.contains(dir.toLowerCase())) {
            throw new IllegalArgumentException("Invalid sort direction '" + dir + "'");
        }
        return Sort.Direction.fromString(dir);
    }
}
