package com.expensetracker.web.error;

import com.expensetracker.service.ExpenseNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates exceptions into the uniform {@link ApiError} body
 * ({@code docs/api-contracts.md}).
 *
 * <ul>
 *   <li>Bean Validation failures on {@code @Valid} bodies → <b>400</b> with one
 *       {@code fieldError} per rejected field.
 *   <li>Unparseable JSON, an unknown {@code category}, or a malformed {@code date}
 *       fail message binding → <b>400</b>.
 *   <li>A bad {@code UUID} path variable, or an invalid query param (unknown
 *       {@code sort} field/direction, negative {@code page}, zero {@code size}) →
 *       <b>400</b>.
 *   <li>{@link ExpenseNotFoundException} → <b>404</b>.
 * </ul>
 *
 * <p>Truly unexpected errors fall through to Spring's default 500 handling.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        // Mirror the contract example ("amount must be greater than 0").
        String message = fieldErrors.isEmpty()
                ? "Validation failed"
                : fieldErrors.get(0).field() + " " + fieldErrors.get(0).message();
        return build(HttpStatus.BAD_REQUEST, message, request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'", request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        // Bad list query params (invalid sort field/direction, negative page,
        // non-positive size) are signalled by ExpenseQuery as IllegalArgumentException.
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ExpenseNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ExpenseNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String message, HttpServletRequest request, List<ApiError.FieldError> fieldErrors) {
        ApiError body = new ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
