package com.expensetracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expensetracker.repository.ExpenseRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Explicit money / decimal precision suite (P9-5) — the named, traceable guard for
 * {@code docs/testing-plan.md} §6. Money is exact decimal end-to-end
 * ({@code BigDecimal} / {@code NUMERIC(12,2)}, INR, two places, {@code > 0}); this
 * suite walks that checklist <b>through the live HTTP API</b> against a real
 * Flyway-migrated Postgres (Testcontainers), so it proves the contract on the wire
 * rather than at any single layer in isolation.
 *
 * <p>The individual layers are already pinned by focused tests
 * ({@code ExpenseEntityTest} storage round-trip, {@code SummaryPrecisionIntegrationTest}
 * aggregation, {@code ExpenseCsvWriterTest} CSV, {@code money.test.ts} frontend
 * display). This suite consolidates the same invariants into one end-to-end
 * checklist mapped 1:1 onto §6:
 *
 * <ol>
 *   <li><b>Storage</b> — POST then GET round-trips an amount with no float drift,
 *       at scale 2 on the wire.</li>
 *   <li><b>Aggregation</b> — {@code /summary} and {@code /summary/by-category} sum
 *       exactly (the classic {@code 0.10 + 0.20 = 0.30} trap and many-small-amount
 *       accumulation), category totals reconcile to the grand total, and percents
 *       are computed from exact totals and sum to ~100% under the documented
 *       half-up rounding.</li>
 *   <li><b>Boundaries</b> — {@code 0.01} accepted; {@code 0} and negative rejected
 *       with the uniform error; high-value amounts within {@code NUMERIC(12,2)}
 *       round-trip.</li>
 *   <li><b>CSV</b> — every amount renders with exactly two decimals, never
 *       scientific notation.</li>
 * </ol>
 *
 * <p>(The fifth §6 item, frontend INR display, is a pure-JS concern covered by
 * {@code frontend/src/lib/money.test.ts}.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("Money/decimal precision suite (testing-plan §6)")
class MoneyPrecisionSuiteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpenseRepository repository;

    // Parses JSON numbers as BigDecimal (not double) so money assertions read the
    // exact value on the wire rather than through a lossy IEEE-754 re-parse.
    private final ObjectMapper bigDecimalMapper =
            new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    // A single month so the aggregation cases share one inclusive range.
    private static final String FROM = "2026-06-01";
    private static final String TO = "2026-06-30";

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    // --- helpers --------------------------------------------------------------

    private static String requestBody(String amount, String date, String category) {
        return "{\"amount\":%s,\"date\":\"%s\",\"category\":\"%s\"}".formatted(amount, date, category);
    }

    /** Creates an expense via the API and returns the persisted id. */
    private UUID createExpense(String amount, String date, String category) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(amount, date, category)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    /** Reads a named numeric field of a JSON response as an exact BigDecimal. */
    private BigDecimal decimalField(String json, String field) throws Exception {
        return bigDecimalMapper.readTree(json).get(field).decimalValue();
    }

    /**
     * Drives the streaming export endpoint end to end: the body comes from a
     * {@code StreamingResponseBody}, so the request starts async and must be
     * re-dispatched before the CSV is on the wire.
     */
    private MockHttpServletResponse exportResponse(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult started =
                mockMvc.perform(request).andExpect(request().asyncStarted()).andReturn();
        return mockMvc.perform(asyncDispatch(started)).andReturn().getResponse();
    }

    // === §6.1 Storage round-trip =============================================

    @Nested
    @DisplayName("Storage: NUMERIC(12,2)/BigDecimal round-trips exactly")
    class Storage {

        @Test
        void smallestAmountRoundTripsAtScaleTwoWithNoDrift() throws Exception {
            UUID id = createExpense("0.01", "2026-06-10", "OTHER");

            MvcResult result = mockMvc.perform(get("/api/expenses/{id}", id))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            assertThat(decimalField(json, "amount")).isEqualByComparingTo("0.01");
            // Serialized at scale 2 on the wire (N.NN), never a binary-float artifact.
            assertThat(json).contains("\"amount\":0.01");
        }

        @Test
        void wholeRupeeAmountRoundTripsWithTwoDecimalScale() throws Exception {
            // 1500 in → 1500.00 back: NUMERIC(12,2) keeps scale 2 so the wire reads N.NN.
            UUID id = createExpense("1500", "2026-06-10", "GROCERIES");

            MvcResult result = mockMvc.perform(get("/api/expenses/{id}", id))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            assertThat(decimalField(json, "amount")).isEqualByComparingTo("1500.00");
            // The wire text carries the two decimals (1500.00), not 1500 or 1.5E3.
            assertThat(json).contains("\"amount\":1500.00");
        }

        @Test
        void highValueAmountWithinRangeRoundTripsExactly() throws Exception {
            // Top of the NUMERIC(12,2) range (ceiling 9_999_999_999.99).
            UUID id = createExpense("9999999999.99", "2026-06-10", "RENT");

            MvcResult result = mockMvc.perform(get("/api/expenses/{id}", id))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            assertThat(decimalField(json, "amount")).isEqualByComparingTo("9999999999.99");
            // Plain decimal text, never scientific notation (e.g. 1.0E10).
            assertThat(json).contains("\"amount\":9999999999.99");
        }
    }

    // === §6.2 Aggregation ====================================================

    @Nested
    @DisplayName("Aggregation: sums exact, percents reconcile to ~100%")
    class Aggregation {

        @Test
        void summaryAddsTheClassicFloatTrapExactly() throws Exception {
            // 0.10 + 0.20 = 0.30 — the canonical IEEE-754 drift case.
            createExpense("0.10", "2026-06-05", "FOOD");
            createExpense("0.20", "2026-06-06", "FOOD");

            MvcResult result = mockMvc.perform(
                            get("/api/summary").param("from", FROM).param("to", TO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(2))
                    .andReturn();

            assertThat(decimalField(result.getResponse().getContentAsString(), "total"))
                    .isEqualByComparingTo("0.30");
        }

        @Test
        void summaryAccumulatesManySmallAmountsWithoutDrift() throws Exception {
            // 100 rows of 0.01 must total exactly 1.00 — small amounts compound float error.
            for (int i = 0; i < 100; i++) {
                createExpense("0.01", "2026-06-10", "OTHER");
            }

            MvcResult result = mockMvc.perform(
                            get("/api/summary").param("from", FROM).param("to", TO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(100))
                    .andReturn();

            assertThat(decimalField(result.getResponse().getContentAsString(), "total"))
                    .isEqualByComparingTo("1.00");
        }

        @Test
        void byCategoryTotalsReconcileToTheGrandTotalAndPercentsSumToHundred() throws Exception {
            // Mixed fractional amounts; per-category sums must reconcile exactly to the
            // headline total, and the half-up percents must sum to ~100%.
            createExpense("10.10", "2026-06-01", "FOOD");
            createExpense("20.20", "2026-06-02", "FOOD");
            createExpense("0.05", "2026-06-03", "TRANSPORT");
            createExpense("0.05", "2026-06-04", "TRANSPORT");
            createExpense("33.33", "2026-06-05", "GROCERIES");

            MvcResult result = mockMvc.perform(
                            get("/api/summary/by-category").param("from", FROM).param("to", TO))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode body = bigDecimalMapper.readTree(result.getResponse().getContentAsString());

            // Grand total is exact (10.10 + 20.20 + 0.05 + 0.05 + 33.33 = 63.73).
            assertThat(body.get("total").decimalValue()).isEqualByComparingTo("63.73");

            // Per-category totals reconcile to the grand total.
            BigDecimal sumOfCategoryTotals = BigDecimal.ZERO;
            BigDecimal sumOfPercents = BigDecimal.ZERO;
            for (JsonNode cat : body.get("categories")) {
                sumOfCategoryTotals = sumOfCategoryTotals.add(cat.get("total").decimalValue());
                sumOfPercents = sumOfPercents.add(cat.get("percent").decimalValue());
            }
            assertThat(sumOfCategoryTotals).isEqualByComparingTo("63.73");

            // Percents are half-up to 2 decimals from exact totals; they sum to ~100
            // (tiny rounding residue allowed, the documented rule).
            assertThat(sumOfPercents).isBetween(new BigDecimal("99.95"), new BigDecimal("100.05"));
        }

        @Test
        void byCategorySingleCategoryIsExactlyOneHundredPercent() throws Exception {
            createExpense("250.75", "2026-06-01", "RENT");

            mockMvc.perform(get("/api/summary/by-category").param("from", FROM).param("to", TO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categories", hasSize(1)))
                    .andExpect(jsonPath("$.categories[0].percent").value(100.00));
        }
    }

    // === §6.3 Boundaries =====================================================

    @Nested
    @DisplayName("Boundaries: 0.01 accepted; 0 and negative rejected")
    class Boundaries {

        @Test
        void acceptsSmallestPositiveAmount() throws Exception {
            mockMvc.perform(post("/api/expenses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody("0.01", "2026-06-10", "OTHER")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(0.01));
        }

        @Test
        void rejectsZeroAmountWithUniformError() throws Exception {
            mockMvc.perform(post("/api/expenses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody("0", "2026-06-10", "FOOD")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("must be greater than 0"));
        }

        @Test
        void rejectsNegativeAmountWithUniformError() throws Exception {
            mockMvc.perform(post("/api/expenses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody("-0.01", "2026-06-10", "FOOD")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
        }

        @Test
        void acceptsHighValueAmountWithinNumericRange() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/expenses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody("9999999999.99", "2026-06-10", "RENT")))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Read as exact BigDecimal (not a lossy double) to confirm full-precision accept.
            assertThat(decimalField(result.getResponse().getContentAsString(), "amount"))
                    .isEqualByComparingTo("9999999999.99");
        }
    }

    // === §6.4 CSV: two decimals, no scientific notation ======================

    @Nested
    @DisplayName("CSV: amounts always two decimals, never scientific notation")
    class Csv {

        @Test
        void exportRendersAmountsWithTwoDecimalsAndNoExponent() throws Exception {
            // Round-trips through real Postgres: a high NUMERIC(12,2) value, a whole
            // rupee, and the smallest positive amount must all print as plain
            // 2-decimal text — never an exponent (e.g. 1.0E10).
            createExpense("9999999999.99", "2026-06-01", "RENT");
            createExpense("1500", "2026-06-02", "RENT");
            createExpense("0.01", "2026-06-03", "OTHER");

            MockHttpServletResponse response = exportResponse(get("/api/expenses/export")
                    .param("from", FROM)
                    .param("to", TO)
                    .param("sort", "date,asc"));

            assertThat(response.getContentType()).contains("text/csv");
            String[] lines = response.getContentAsString().split("\r\n");
            assertThat(lines).hasSize(4);
            assertThat(lines[0]).isEqualTo("id,date,category,amount");
            assertThat(lines[1].split(",")[3]).isEqualTo("9999999999.99");
            assertThat(lines[2].split(",")[3]).isEqualTo("1500.00");
            assertThat(lines[3].split(",")[3]).isEqualTo("0.01");
            for (int i = 1; i < lines.length; i++) {
                assertThat(lines[i].split(",")[3]).doesNotContainIgnoringCase("e");
            }
        }
    }
}
