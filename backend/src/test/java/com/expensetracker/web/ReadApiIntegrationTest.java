package com.expensetracker.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expensetracker.TestcontainersConfiguration;
import com.expensetracker.domain.Category;
import com.expensetracker.domain.Expense;
import com.expensetracker.repository.ExpenseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end integration tests (P3-6) for the read/aggregation endpoints — the
 * filtered/paginated list ({@code GET /api/expenses}) and the three summary
 * endpoints ({@code /summary}, {@code /summary/by-category}, {@code
 * /summary/trend}) — driving the <b>full stack</b> (controller → service →
 * repository → real Postgres via Testcontainers) through {@link MockMvc}.
 *
 * <p>Where the P3-1..P3-5 repository tests pin the SQL with explicit ranges, and
 * the {@code @WebMvcTest} slices pin the HTTP wire shape with a mocked service,
 * this fills the gap the phase exit calls for: that the wired endpoints return
 * <em>correct</em> aggregated/filtered/paginated data across the edge cases from
 * {@code docs/testing-plan.md} §2 — <b>empty month, single category, mixed</b> —
 * including the contract's current-month defaulting (only reachable through the
 * service, which resolves it against {@code LocalDate.now()}).
 *
 * <p>The fixed-range cases seed a deterministic set spanning May–July 2026 and
 * query explicit {@code from}/{@code to}, so they are independent of the wall
 * clock. The defaulting cases seed into {@code YearMonth.now()} and omit the
 * range, exercising the same resolution the live API uses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReadApiIntegrationTest {

    // Fixed seed spanning three months, with rows on the filter boundaries
    // (2026-06-01, 2026-06-30) and the precision-sensitive 0.01 amount.
    private static final LocalDate MAY_31 = LocalDate.of(2026, 5, 31);
    private static final LocalDate JUN_01 = LocalDate.of(2026, 6, 1);
    private static final LocalDate JUN_15 = LocalDate.of(2026, 6, 15);
    private static final LocalDate JUN_30 = LocalDate.of(2026, 6, 30);
    private static final LocalDate JUL_01 = LocalDate.of(2026, 7, 1);

    private static final String JUNE_FROM = "2026-06-01";
    private static final String JUNE_TO = "2026-06-30";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private void seedThreeMonths() {
        repository.saveAll(List.of(
                expense("100.00", MAY_31, Category.FOOD),
                expense("50.00", JUN_01, Category.GROCERIES),
                expense("200.00", JUN_15, Category.FOOD),
                expense("0.01", JUN_30, Category.TRANSPORT),
                expense("1500.00", JUL_01, Category.RENT)));
    }

    private static Expense expense(String amount, LocalDate date, Category category) {
        return new Expense(new BigDecimal(amount), date, category);
    }

    // --- GET /api/expenses — list / filter / pagination -----------------------

    @Test
    void listWithExplicitRangeReturnsOnlyInRangeRowsWithContractEnvelope() throws Exception {
        seedThreeMonths();

        // June bracket [01,30] excludes May 31 and Jul 01; default sort date,desc.
        mockMvc.perform(get("/api/expenses").param("from", JUNE_FROM).param("to", JUNE_TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.sort").value("date,desc"))
                // date,desc → 06-30, 06-15, 06-01.
                .andExpect(jsonPath("$.content[0].date").value("2026-06-30"))
                .andExpect(jsonPath("$.content[0].amount").value(0.01))
                .andExpect(jsonPath("$.content[1].date").value("2026-06-15"))
                .andExpect(jsonPath("$.content[2].date").value("2026-06-01"));
    }

    @Test
    void listFiltersBySingleCategoryAcrossTheWholeSpan() throws Exception {
        seedThreeMonths();

        // Wide range so the category filter (not the date) is what narrows it:
        // FOOD has the May 31 and Jun 15 rows.
        mockMvc.perform(get("/api/expenses")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .param("category", "FOOD")
                        .param("sort", "amount,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].amount").value(100.00))
                .andExpect(jsonPath("$.content[0].category").value("FOOD"))
                .andExpect(jsonPath("$.content[1].amount").value(200.00));
    }

    @Test
    void listAppliesInclusiveAmountRange() throws Exception {
        seedThreeMonths();

        // [50,200] over the full span keeps 50.00, 100.00, 200.00; drops 0.01 and 1500.00.
        mockMvc.perform(get("/api/expenses")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .param("minAmount", "50.00")
                        .param("maxAmount", "200.00")
                        .param("sort", "amount,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].amount").value(50.00))
                .andExpect(jsonPath("$.content[1].amount").value(100.00))
                .andExpect(jsonPath("$.content[2].amount").value(200.00));
    }

    @Test
    void listPaginatesAndReportsTotalsAcrossPages() throws Exception {
        seedThreeMonths();

        // size 2 over the 5-row full span, sorted date,asc → page 0 holds the
        // two earliest rows; the envelope still reports the full totals.
        mockMvc.perform(get("/api/expenses")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .param("sort", "date,asc")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].date").value("2026-05-31"))
                .andExpect(jsonPath("$.content[1].date").value("2026-06-01"));

        // The trailing page holds the single remaining row.
        mockMvc.perform(get("/api/expenses")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .param("sort", "date,asc")
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.content[0].date").value("2026-07-01"));
    }

    @Test
    void listDefaultsToCurrentMonthWhenRangeOmitted() throws Exception {
        // Seed one row in the current month and one in a neighbouring month; with
        // no from/to the contract scopes the list to the current month only.
        YearMonth now = YearMonth.now();
        LocalDate inMonth = now.atDay(15);
        LocalDate lastMonth = now.minusMonths(1).atDay(15);
        repository.saveAll(
                List.of(expense("123.45", inMonth, Category.FOOD), expense("999.99", lastMonth, Category.RENT)));

        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(123.45))
                .andExpect(jsonPath("$.content[0].date").value(inMonth.toString()));
    }

    @Test
    void listReturnsEmptyEnvelopeForAnEmptyMonth() throws Exception {
        seedThreeMonths();

        // A month with no rows still returns the well-formed envelope, not an error.
        mockMvc.perform(get("/api/expenses").param("from", "2030-01-01").param("to", "2030-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.sort").value("date,desc"));
    }

    // --- GET /api/expenses/export — CSV, same filters, no pagination ----------

    @Test
    void exportReturnsCsvForTheFilteredRangeWithHeadersAndNoPagination() throws Exception {
        seedThreeMonths();

        // June bracket [01,30] excludes May 31 and Jul 01; default sort date,desc.
        // No page/size: every match is streamed as CSV rows under the header.
        MvcResult started = mockMvc.perform(
                        get("/api/expenses/export").param("from", JUNE_FROM).param("to", JUNE_TO))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(
                                "Content-Disposition",
                                org.hamcrest.Matchers.startsWith("attachment; filename=\"expenses-")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String[] lines = body.split("\r\n");
        // Header + the three June rows, date,desc → 06-30, 06-15, 06-01.
        org.assertj.core.api.Assertions.assertThat(lines).hasSize(4);
        org.assertj.core.api.Assertions.assertThat(lines[0]).isEqualTo("id,date,category,amount");
        org.assertj.core.api.Assertions.assertThat(lines[1]).endsWith("2026-06-30,TRANSPORT,0.01");
        org.assertj.core.api.Assertions.assertThat(lines[2]).endsWith("2026-06-15,FOOD,200.00");
        org.assertj.core.api.Assertions.assertThat(lines[3]).endsWith("2026-06-01,GROCERIES,50.00");
    }

    @Test
    void exportAppliesCategoryFilterAcrossTheWholeSpanIgnoringPageSize() throws Exception {
        seedThreeMonths();

        // Wide range + category FOOD → the May 31 and Jun 15 rows, sorted amount,asc.
        // A tiny size param must be ignored (export never paginates): both rows appear.
        MvcResult started = mockMvc.perform(get("/api/expenses/export")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .param("category", "FOOD")
                        .param("sort", "amount,asc")
                        .param("size", "1"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String[] lines = body.split("\r\n");
        org.assertj.core.api.Assertions.assertThat(lines).hasSize(3);
        org.assertj.core.api.Assertions.assertThat(lines[1]).endsWith("2026-05-31,FOOD,100.00");
        org.assertj.core.api.Assertions.assertThat(lines[2]).endsWith("2026-06-15,FOOD,200.00");
    }

    @Test
    void exportReturnsHeaderOnlyForAnEmptyMonth() throws Exception {
        seedThreeMonths();

        MvcResult started = mockMvc.perform(
                        get("/api/expenses/export").param("from", "2030-01-01").param("to", "2030-01-31"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body).isEqualTo("id,date,category,amount\r\n");
    }

    // --- GET /api/summary -----------------------------------------------------

    @Test
    void summaryTotalsAndCountsTheMixedMonthExactly() throws Exception {
        seedThreeMonths();

        // June: 50.00 + 200.00 + 0.01 = 250.01 over 3 rows.
        mockMvc.perform(get("/api/summary").param("from", JUNE_FROM).param("to", JUNE_TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(JUNE_FROM))
                .andExpect(jsonPath("$.to").value(JUNE_TO))
                .andExpect(jsonPath("$.total").value(250.01))
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void summaryReturnsZeroTotalForAnEmptyMonth() throws Exception {
        seedThreeMonths();

        // Empty period: total normalizes to 0.00 (scale 2), count 0 — no nulls.
        mockMvc.perform(get("/api/summary").param("from", "2030-01-01").param("to", "2030-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0.00))
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void summaryDefaultsToCurrentMonthWhenRangeOmitted() throws Exception {
        YearMonth now = YearMonth.now();
        repository.saveAll(List.of(
                expense("10.00", now.atDay(2), Category.FOOD),
                expense("5.50", now.atDay(20), Category.FOOD),
                // Out of the current month — must be excluded by the default range.
                expense("777.00", now.minusMonths(1).atEndOfMonth(), Category.RENT)));

        mockMvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(now.atDay(1).toString()))
                .andExpect(jsonPath("$.to").value(now.atEndOfMonth().toString()))
                .andExpect(jsonPath("$.total").value(15.50))
                .andExpect(jsonPath("$.count").value(2));
    }

    // --- GET /api/summary/by-category -----------------------------------------

    @Test
    void byCategoryBreaksDownMixedMonthLargestFirstWithPercentShares() throws Exception {
        seedThreeMonths();

        // June: FOOD 200.00, GROCERIES 50.00, TRANSPORT 0.01; grand total 250.01.
        // Percents (half-up, 2dp): 200.00/250.01=79.997→80.00, 50/250.01=19.999→20.00,
        // 0.01/250.01=0.004→0.00.
        mockMvc.perform(get("/api/summary/by-category").param("from", JUNE_FROM).param("to", JUNE_TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(JUNE_FROM))
                .andExpect(jsonPath("$.to").value(JUNE_TO))
                .andExpect(jsonPath("$.total").value(250.01))
                .andExpect(jsonPath("$.categories", hasSize(3)))
                .andExpect(jsonPath("$.categories[0].category").value("FOOD"))
                .andExpect(jsonPath("$.categories[0].total").value(200.00))
                .andExpect(jsonPath("$.categories[0].count").value(1))
                .andExpect(jsonPath("$.categories[0].percent").value(80.00))
                .andExpect(jsonPath("$.categories[1].category").value("GROCERIES"))
                .andExpect(jsonPath("$.categories[1].total").value(50.00))
                .andExpect(jsonPath("$.categories[1].percent").value(20.00))
                .andExpect(jsonPath("$.categories[2].category").value("TRANSPORT"))
                .andExpect(jsonPath("$.categories[2].total").value(0.01))
                .andExpect(jsonPath("$.categories[2].percent").value(0.00));
    }

    @Test
    void byCategoryWithASingleCategorySumsItsRowsToA100PercentShare() throws Exception {
        // The summary endpoints scope by date range only (no category param), so a
        // single-category breakdown comes from a window holding one category's rows.
        // Two FOOD rows in one day: 100.00 + 200.00 = 300.00, count 2, and as the
        // only slice it must be exactly 100% of the total.
        repository.saveAll(List.of(expense("100.00", JUN_15, Category.FOOD), expense("200.00", JUN_15, Category.FOOD)));

        mockMvc.perform(get("/api/summary/by-category").param("from", JUNE_FROM).param("to", JUNE_TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(300.00))
                .andExpect(jsonPath("$.categories", hasSize(1)))
                .andExpect(jsonPath("$.categories[0].category").value("FOOD"))
                .andExpect(jsonPath("$.categories[0].total").value(300.00))
                .andExpect(jsonPath("$.categories[0].count").value(2))
                .andExpect(jsonPath("$.categories[0].percent").value(100.00));
    }

    @Test
    void byCategoryReturnsZeroTotalAndNoSlicesForAnEmptyMonth() throws Exception {
        seedThreeMonths();

        mockMvc.perform(get("/api/summary/by-category")
                        .param("from", "2030-01-01")
                        .param("to", "2030-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0.00))
                .andExpect(jsonPath("$.categories", hasSize(0)));
    }

    // --- GET /api/summary/trend -----------------------------------------------

    @Test
    void trendBucketsByDayWithinAMonthAscending() throws Exception {
        seedThreeMonths();

        // Within a single month the default granularity is day; June has three
        // distinct spend days, ascending.
        mockMvc.perform(get("/api/summary/trend").param("from", JUNE_FROM).param("to", JUNE_TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(JUNE_FROM))
                .andExpect(jsonPath("$.to").value(JUNE_TO))
                .andExpect(jsonPath("$.granularity").value("day"))
                .andExpect(jsonPath("$.points", hasSize(3)))
                .andExpect(jsonPath("$.points[0].period").value("2026-06-01"))
                .andExpect(jsonPath("$.points[0].total").value(50.00))
                .andExpect(jsonPath("$.points[1].period").value("2026-06-15"))
                .andExpect(jsonPath("$.points[1].total").value(200.00))
                .andExpect(jsonPath("$.points[2].period").value("2026-06-30"))
                .andExpect(jsonPath("$.points[2].total").value(0.01));
    }

    @Test
    void trendBucketsByMonthAcrossALongerSpanAscending() throws Exception {
        seedThreeMonths();

        // Across more than one month the default granularity is month: May 100.00,
        // June 250.01, July 1500.00.
        mockMvc.perform(get("/api/summary/trend").param("from", "2026-01-01").param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity").value("month"))
                .andExpect(jsonPath("$.points", hasSize(3)))
                .andExpect(jsonPath("$.points[0].period").value("2026-05"))
                .andExpect(jsonPath("$.points[0].total").value(100.00))
                .andExpect(jsonPath("$.points[1].period").value("2026-06"))
                .andExpect(jsonPath("$.points[1].total").value(250.01))
                .andExpect(jsonPath("$.points[2].period").value("2026-07"))
                .andExpect(jsonPath("$.points[2].total").value(1500.00));
    }

    @Test
    void trendHonoursAnExplicitGranularityOverTheDefault() throws Exception {
        seedThreeMonths();

        // Force month buckets even though the range is a single month: the one
        // June bucket sums 50.00 + 200.00 + 0.01 = 250.01.
        mockMvc.perform(get("/api/summary/trend")
                        .param("from", JUNE_FROM)
                        .param("to", JUNE_TO)
                        .param("granularity", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity").value("month"))
                .andExpect(jsonPath("$.points", hasSize(1)))
                .andExpect(jsonPath("$.points[0].period").value("2026-06"))
                .andExpect(jsonPath("$.points[0].total").value(250.01));
    }

    @Test
    void trendReturnsNoPointsForAnEmptyMonth() throws Exception {
        seedThreeMonths();

        // Empty buckets are omitted, so an empty period yields an empty series.
        mockMvc.perform(get("/api/summary/trend").param("from", "2030-01-01").param("to", "2030-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity").value("day"))
                .andExpect(jsonPath("$.points", hasSize(0)));
    }

    @Test
    void trendDefaultsToCurrentMonthDayBucketsWhenRangeOmitted() throws Exception {
        YearMonth now = YearMonth.now();
        repository.saveAll(List.of(
                expense("12.00", now.atDay(3), Category.FOOD),
                expense("8.00", now.atDay(3), Category.TRANSPORT),
                // Excluded by the default current-month range.
                expense("500.00", now.minusMonths(1).atDay(10), Category.RENT)));

        // No range → current month; a single month → day granularity. The two
        // same-day rows collapse into one bucket summing 20.00.
        mockMvc.perform(get("/api/summary/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(now.atDay(1).toString()))
                .andExpect(jsonPath("$.to").value(now.atEndOfMonth().toString()))
                .andExpect(jsonPath("$.granularity").value("day"))
                .andExpect(jsonPath("$.points", hasSize(1)))
                .andExpect(jsonPath("$.points[0].period").value(now.atDay(3).toString()))
                .andExpect(jsonPath("$.points[0].total").value(20.00));
    }
}
