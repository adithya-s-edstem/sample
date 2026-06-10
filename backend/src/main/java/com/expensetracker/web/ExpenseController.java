package com.expensetracker.web;

import com.expensetracker.service.ExpenseService;
import com.expensetracker.web.dto.ExpenseRequest;
import com.expensetracker.web.dto.ExpenseResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD endpoints for expenses, matching {@code docs/api-contracts.md}:
 *
 * <ul>
 *   <li>{@code POST   /api/expenses}      → 201 + {@link ExpenseResponse}
 *   <li>{@code GET    /api/expenses/{id}} → 200 (404 if missing)
 *   <li>{@code PUT    /api/expenses/{id}} → 200 (404 if missing)
 *   <li>{@code DELETE /api/expenses/{id}} → 204 (404 if missing)
 * </ul>
 *
 * <p>Thin layer over {@link ExpenseService}. Request bodies are {@code @Valid}
 * so the Bean Validation constraints added in P2-4 take effect here; that task
 * also adds the global handler that renders the uniform error shape (400 for
 * validation, 404 for {@code ExpenseNotFoundException}).
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public ExpenseResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable UUID id, @Valid @RequestBody ExpenseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
