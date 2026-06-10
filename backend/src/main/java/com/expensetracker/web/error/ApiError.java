package com.expensetracker.web.error;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error body returned by {@link GlobalExceptionHandler}, matching the
 * error shape in {@code docs/api-contracts.md}:
 *
 * <pre>{ timestamp, status, error, message, path, fieldErrors[] }</pre>
 *
 * <p>{@code fieldErrors} is always present (empty for non-validation errors) so
 * the shape is uniform across every status. {@code timestamp} is an ISO-8601
 * UTC instant; {@code error} is the HTTP reason phrase.
 */
public record ApiError(
        Instant timestamp, int status, String error, String message, String path, List<FieldError> fieldErrors) {

    /** A single rejected field (Bean Validation violation). */
    public record FieldError(String field, String message) {}
}
