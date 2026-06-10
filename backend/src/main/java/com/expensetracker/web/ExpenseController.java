package com.expensetracker.web;

import com.expensetracker.domain.Category;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.web.csv.ExpenseCsvWriter;
import com.expensetracker.web.dto.ExpenseQuery;
import com.expensetracker.web.dto.ExpenseRequest;
import com.expensetracker.web.dto.ExpenseResponse;
import com.expensetracker.web.dto.PageResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * Filtered, sorted, paginated list ({@code GET /api/expenses}). Every query
     * param is optional; the contract's defaults (current-month range, {@code
     * date,desc} sort, page {@code 0} / size {@code 50}) are applied in
     * {@link ExpenseQuery}. A malformed {@code date}/{@code category}/number or an
     * invalid {@code sort}/{@code page}/{@code size} surfaces as a uniform 400 via
     * the global handler.
     */
    @GetMapping
    public PageResponse<ExpenseResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return service.list(new ExpenseQuery(from, to, category, minAmount, maxAmount, q, sort, page, size));
    }

    /**
     * CSV export ({@code GET /api/expenses/export}, P4-1). Accepts the <b>same
     * filter params as the list</b> ({@code from}, {@code to}, {@code category},
     * {@code minAmount}, {@code maxAmount}, {@code q}) with the same current-month
     * defaulting and {@code date,desc} default ordering, but <b>no pagination</b> —
     * it exports every match.
     *
     * <p>Responds {@code 200} with {@code Content-Type: text/csv} and a {@code
     * Content-Disposition: attachment} filename of {@code expenses-YYYY-MM-DD.csv}
     * (today's date). Body is {@code id,date,category,amount}, amounts at two
     * decimals (see {@link ExpenseCsvWriter}). A malformed param surfaces as a
     * uniform 400 via the global handler, exactly as on the list endpoint.
     */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort) {
        // Export reuses the list query but never paginates, so page/size are unset.
        List<ExpenseResponse> rows =
                service.export(new ExpenseQuery(from, to, category, minAmount, maxAmount, q, sort, null, null));
        String csv = ExpenseCsvWriter.write(rows);
        String filename = "expenses-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
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
