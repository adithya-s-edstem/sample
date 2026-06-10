package com.expensetracker.web.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Paginated list response, matching the {@code GET /api/expenses} body in
 * {@code docs/api-contracts.md}:
 *
 * <pre>{ content, page, size, totalElements, totalPages, sort }</pre>
 *
 * <p>This is a deliberately flat projection of Spring Data's {@link Page} — only
 * the fields the contract pins, so the wire shape is stable regardless of how the
 * {@code Page} implementation evolves. {@code sort} is the echoed-back sort string
 * (e.g. {@code "date,desc"}) the request resolved to, not Spring's {@code Sort}
 * object representation.
 *
 * @param <T> the element type carried in {@code content}
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, String sort) {

    /** Builds a response from a {@link Page} plus the resolved {@code sort} string. */
    public static <T> PageResponse<T> of(Page<T> page, String sort) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                sort);
    }
}
