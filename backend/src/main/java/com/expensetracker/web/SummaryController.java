package com.expensetracker.web;

import com.expensetracker.service.ExpenseService;
import com.expensetracker.web.dto.CategorySummaryResponse;
import com.expensetracker.web.dto.Granularity;
import com.expensetracker.web.dto.SummaryQuery;
import com.expensetracker.web.dto.SummaryResponse;
import com.expensetracker.web.dto.TrendQuery;
import com.expensetracker.web.dto.TrendResponse;
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
 *   <li>{@code GET /api/summary/trend} → {@link TrendResponse} (spending over
 *       time, day/month buckets, for the bar/line chart)
 * </ul>
 *
 * <p>This controller is introduced for P3-2 ({@code /summary}); P3-3 adds
 * {@code /summary/by-category} and P3-4 adds {@code /summary/trend}.
 *
 * <p>Thin layer over {@link ExpenseService}. Range params are optional and default
 * to the current calendar month (resolved in {@link SummaryQuery}/{@link
 * TrendQuery}); a malformed {@code from}/{@code to} or an unknown
 * {@code granularity} surfaces as a uniform 400 via the global handler.
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

    @GetMapping("/trend")
    public TrendResponse trend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String granularity) {
        // granularity is bound as a raw token and parsed case-insensitively here;
        // an unknown value raises IllegalArgumentException → uniform 400.
        return service.summaryTrend(new TrendQuery(from, to, Granularity.fromParam(granularity)));
    }
}
