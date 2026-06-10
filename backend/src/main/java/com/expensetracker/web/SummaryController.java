package com.expensetracker.web;

import com.expensetracker.service.ExpenseService;
import com.expensetracker.web.dto.CategorySummaryResponse;
import com.expensetracker.web.dto.SummaryQuery;
import com.expensetracker.web.dto.SummaryResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aggregation endpoints, matching section 3 of {@code docs/api-contracts.md}. The
 * backend computes these so the frontend receives ready-to-chart JSON.
 *
 * <ul>
 *   <li>{@code GET /api/summary} → headline {@link SummaryResponse} (total + count)
 *   <li>{@code GET /api/summary/by-category} → {@link CategorySummaryResponse}
 *       (per-category totals + percent share, for the donut)
 * </ul>
 *
 * <p>This controller is introduced for P3-2 ({@code /summary}); P3-3 adds
 * {@code /summary/by-category}. {@code /summary/trend} (P3-4) lands here next.
 *
 * <p>Thin layer over {@link ExpenseService}. Both range params are optional and
 * default to the current calendar month (resolved in {@link SummaryQuery}); a
 * malformed {@code from}/{@code to} surfaces as a uniform 400 via the global
 * handler.
 */
@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final ExpenseService service;

    public SummaryController(ExpenseService service) {
        this.service = service;
    }

    @GetMapping
    public SummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(new SummaryQuery(from, to));
    }

    @GetMapping("/by-category")
    public CategorySummaryResponse byCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summaryByCategory(new SummaryQuery(from, to));
    }
}
