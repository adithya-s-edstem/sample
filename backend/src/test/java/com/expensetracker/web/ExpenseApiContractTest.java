package com.expensetracker.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expensetracker.TestcontainersConfiguration;
import com.expensetracker.repository.ExpenseRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end API contract tests (P2-7) validating the CRUD + reference endpoints
 * against {@code docs/api-contracts.md}.
 *
 * <p>Unlike the P2-6 {@code @WebMvcTest} slices (which mock the service), this
 * drives the <b>full stack</b> — controller → service → mapper → repository →
 * real Postgres (Flyway-migrated, via Testcontainers) — through {@link MockMvc}.
 * It pins the wire contract exactly: HTTP status codes, response/error JSON
 * shapes and field types (UUID {@code id}, 2-decimal {@code amount}, {@code date}
 * as {@code YYYY-MM-DD}, {@code createdAt}/{@code updatedAt} as ISO-8601 UTC
 * instants), and the uniform error body with {@code fieldErrors[]}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ExpenseApiContractTest {

    /** ISO-8601 instant in UTC, e.g. {@code 2026-06-10T09:15:30.123Z} (Z suffix, never an offset). */
    private static final String ISO_UTC_INSTANT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z";

    private static final String UUID_PATTERN =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpenseRepository repository;

    // Parses JSON numbers as BigDecimal (not double) so money assertions read the
    // exact value on the wire rather than a lossy re-parse.
    private final ObjectMapper bigDecimalMapper =
            new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private static String requestBody(String amount, String date, String category) {
        return "{\"amount\":%s,\"date\":%s,\"category\":%s}".formatted(amount, date, category);
    }

    /** Creates an expense via the API and returns its generated id. */
    private UUID createExpense(String amount, String date, String category) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(amount, "\"" + date + "\"", "\"" + category + "\"")))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    // --- POST /api/expenses ---------------------------------------------------

    @Test
    void postCreatesExpenseReturning201AndContractBody() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("1200.00", "\"2026-06-10\"", "\"GROCERIES\"")))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // id is a generated UUID string.
                .andExpect(jsonPath("$.id", matchesPattern(UUID_PATTERN)))
                // amount is exact decimal, two places.
                .andExpect(jsonPath("$.amount").value(1200.00))
                // date round-trips as YYYY-MM-DD.
                .andExpect(jsonPath("$.date").value("2026-06-10"))
                .andExpect(jsonPath("$.category").value("GROCERIES"))
                // timestamps are ISO-8601 instants in UTC (Z-suffixed).
                .andExpect(jsonPath("$.createdAt", matchesPattern(ISO_UTC_INSTANT)))
                .andExpect(jsonPath("$.updatedAt", matchesPattern(ISO_UTC_INSTANT)));
    }

    @Test
    void postResponseHasExactlyTheContractFields() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("75.50", "\"2026-06-10\"", "\"FOOD\"")))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("id", "amount", "date", "category", "createdAt", "updatedAt");
    }

    @Test
    void postRejectsZeroAmountWithUniformErrorShape() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("0", "\"2026-06-10\"", "\"FOOD\"")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", matchesPattern(ISO_UTC_INSTANT)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("amount must be greater than 0"))
                .andExpect(jsonPath("$.path").value("/api/expenses"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must be greater than 0"));
    }

    @Test
    void postRejectsNegativeAmount() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("-1.00", "\"2026-06-10\"", "\"FOOD\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
    }

    @Test
    void postRejectsMissingFieldsWithOneErrorPerField() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("null", "null", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(3)))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("amount", "date", "category")))
                .andExpect(jsonPath("$.fieldErrors[*].message", everyItem(is("must not be null"))));
    }

    @Test
    void postRejectsUnknownCategory() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("10.00", "\"2026-06-10\"", "\"NOT_A_CATEGORY\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void postRejectsMalformedDate() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("10.00", "\"2026-13-40\"", "\"FOOD\"")))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/expenses/{id} ----------------------------------------------

    @Test
    void getReturns200WithStoredExpense() throws Exception {
        UUID id = createExpense("250.75", "2026-06-01", "HEALTH");

        mockMvc.perform(get("/api/expenses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.amount").value(250.75))
                .andExpect(jsonPath("$.date").value("2026-06-01"))
                .andExpect(jsonPath("$.category").value("HEALTH"))
                .andExpect(jsonPath("$.createdAt", matchesPattern(ISO_UTC_INSTANT)))
                .andExpect(jsonPath("$.updatedAt", matchesPattern(ISO_UTC_INSTANT)));
    }

    @Test
    void getReturns404WithUniformShapeWhenMissing() throws Exception {
        UUID missing = UUID.fromString("9f1c2e7a-3b6d-4f2a-8c11-2a7e9d0b4c55");

        mockMvc.perform(get("/api/expenses/{id}", missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", matchesPattern(ISO_UTC_INSTANT)))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/expenses/" + missing))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void getReturns400ForMalformedUuidPath() throws Exception {
        mockMvc.perform(get("/api/expenses/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // --- PUT /api/expenses/{id} ----------------------------------------------

    @Test
    void putReplacesExpenseReturning200WithUpdatedBody() throws Exception {
        UUID id = createExpense("100.00", "2026-06-01", "FOOD");

        mockMvc.perform(put("/api/expenses/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("999.99", "\"2026-06-20\"", "\"SHOPPING\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.amount").value(999.99))
                .andExpect(jsonPath("$.date").value("2026-06-20"))
                .andExpect(jsonPath("$.category").value("SHOPPING"));

        // Persisted state reflects the update.
        mockMvc.perform(get("/api/expenses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(999.99))
                .andExpect(jsonPath("$.category").value("SHOPPING"));
    }

    @Test
    void putReturns400ForInvalidBody() throws Exception {
        UUID id = createExpense("100.00", "2026-06-01", "FOOD");

        mockMvc.perform(put("/api/expenses/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("0", "\"2026-06-20\"", "\"SHOPPING\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
    }

    @Test
    void putReturns404WhenMissing() throws Exception {
        UUID missing = UUID.fromString("9f1c2e7a-3b6d-4f2a-8c11-2a7e9d0b4c55");

        mockMvc.perform(put("/api/expenses/{id}", missing)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("10.00", "\"2026-06-10\"", "\"FOOD\"")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", matchesPattern(ISO_UTC_INSTANT)))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                // Pin path for the PUT verb too — the not-found URI is echoed back.
                .andExpect(jsonPath("$.path").value("/api/expenses/" + missing))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    // --- DELETE /api/expenses/{id} -------------------------------------------

    @Test
    void deleteReturns204AndRemovesExpense() throws Exception {
        UUID id = createExpense("42.00", "2026-06-05", "OTHER");

        mockMvc.perform(delete("/api/expenses/{id}", id)).andExpect(status().isNoContent());

        // Now gone → 404.
        mockMvc.perform(get("/api/expenses/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        UUID missing = UUID.fromString("9f1c2e7a-3b6d-4f2a-8c11-2a7e9d0b4c55");

        mockMvc.perform(delete("/api/expenses/{id}", missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- Money / decimal precision (round-trip through real Postgres) --------

    @Test
    void acceptsSmallestPositiveAmountAndRoundTripsExactly() throws Exception {
        UUID id = createExpense("0.01", "2026-06-10", "OTHER");

        mockMvc.perform(get("/api/expenses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(0.01));
    }

    @Test
    void preservesWholeRupeeValueExactlyAsJsonNumber() throws Exception {
        // A whole-rupee value must round-trip exactly. amount is a JSON number,
        // so the contract guarantee is numeric equality (1500 == 1500.00), not a
        // particular textual scale; assert it as a number via the matcher.
        UUID id = createExpense("1500", "2026-06-10", "RENT");

        mockMvc.perform(get("/api/expenses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1500.00));
    }

    @Test
    void preservesHighValueWithinNumeric12And2Range() throws Exception {
        // Largest value NUMERIC(12,2) allows: 10 integer digits + 2 fractional.
        UUID id = createExpense("9999999999.99", "2026-06-10", "RENT");

        MvcResult result = mockMvc.perform(get("/api/expenses/{id}", id)).andReturn();
        // Read the raw JSON number as BigDecimal (no intermediate double) so the
        // assertion reflects the exact value on the wire and would catch real
        // float drift — not a value that merely survives a lossy double re-parse.
        JsonNode body = bigDecimalMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("amount").decimalValue()).isEqualByComparingTo("9999999999.99");
    }

    // --- GET /api/expenses (list / filter / paginate) ------------------------

    /** Seeds rows spanning May–July 2026 across categories/amounts for the list tests. */
    private void seedListFixture() throws Exception {
        createExpense("100.00", "2026-05-31", "FOOD");
        createExpense("50.00", "2026-06-01", "GROCERIES");
        createExpense("200.00", "2026-06-15", "FOOD");
        createExpense("0.01", "2026-06-30", "TRANSPORT");
        createExpense("1500.00", "2026-07-01", "RENT");
    }

    @Test
    void listReturnsContractPageShape() throws Exception {
        seedListFixture();

        // Explicit range covering everything so the assertion is independent of "today".
        mockMvc.perform(get("/api/expenses").param("from", "2026-01-01").param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.sort").value("date,desc"));
    }

    @Test
    void listPageBodyHasExactlyTheContractFields() throws Exception {
        seedListFixture();

        MvcResult result = mockMvc.perform(get("/api/expenses").param("from", "2026-01-01"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("content", "page", "size", "totalElements", "totalPages", "sort");
    }

    @Test
    void listDefaultsToCurrentMonth() throws Exception {
        // Two rows in the current month, one outside it; with no from/to the
        // list must scope to the current month only.
        java.time.YearMonth thisMonth = java.time.YearMonth.now();
        createExpense("11.00", thisMonth.atDay(1).toString(), "FOOD");
        createExpense("22.00", thisMonth.atEndOfMonth().toString(), "RENT");
        createExpense("33.00", thisMonth.minusMonths(1).atDay(15).toString(), "OTHER");

        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listFiltersByCategory() throws Exception {
        seedListFixture();

        mockMvc.perform(get("/api/expenses")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .param("category", "FOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].category", everyItem(is("FOOD"))));
    }

    @Test
    void listFiltersByAmountRangeInclusive() throws Exception {
        seedListFixture();

        mockMvc.perform(get("/api/expenses")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .param("minAmount", "50.00")
                        .param("maxAmount", "200.00"))
                .andExpect(status().isOk())
                // 50.00, 100.00, 200.00 are within [50,200]; 0.01 and 1500.00 are not.
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void listSortsByAmountAscending() throws Exception {
        seedListFixture();

        mockMvc.perform(get("/api/expenses")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .param("sort", "amount,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(0.01))
                .andExpect(jsonPath("$.content[4].amount").value(1500.00))
                .andExpect(jsonPath("$.sort").value("amount,asc"));
    }

    @Test
    void listPaginatesWithCappedAndCountedPages() throws Exception {
        seedListFixture();

        mockMvc.perform(get("/api/expenses")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .param("sort", "date,asc")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void listCapsSizeAtMax() throws Exception {
        mockMvc.perform(get("/api/expenses").param("size", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(200));
    }

    @Test
    void listReturns400ForInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/expenses").param("sort", "bogus,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void listReturns400ForNegativePage() throws Exception {
        mockMvc.perform(get("/api/expenses").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listReturns400ForMalformedDateParam() throws Exception {
        mockMvc.perform(get("/api/expenses").param("from", "2026-13-40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // --- GET /api/summary (total + count for a period) -----------------------

    @Test
    void summaryReturnsContractBodyShapeWithExactlyTheContractFields() throws Exception {
        createExpense("12000.00", "2026-06-05", "RENT");
        createExpense("500.00", "2026-06-06", "FOOD");

        MvcResult result = mockMvc.perform(
                        get("/api/summary").param("from", "2026-06-01").param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.from").value("2026-06-01"))
                .andExpect(jsonPath("$.to").value("2026-06-30"))
                .andExpect(jsonPath("$.total").value(12500.00))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("from", "to", "total", "count", "currency");
    }

    @Test
    void summaryDefaultsToCurrentMonth() throws Exception {
        java.time.YearMonth thisMonth = java.time.YearMonth.now();
        // Two rows in the current month, one outside it; with no from/to the
        // summary must aggregate the current month only.
        createExpense("100.00", thisMonth.atDay(1).toString(), "FOOD");
        createExpense("250.00", thisMonth.atEndOfMonth().toString(), "RENT");
        createExpense("999.00", thisMonth.minusMonths(1).atDay(15).toString(), "OTHER");

        mockMvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(thisMonth.atDay(1).toString()))
                .andExpect(jsonPath("$.to").value(thisMonth.atEndOfMonth().toString()))
                .andExpect(jsonPath("$.total").value(350.00))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void summaryReportsZeroTotalAndCountForEmptyPeriod() throws Exception {
        // No rows in range → total 0.00, count 0 (not null / not an error).
        mockMvc.perform(get("/api/summary").param("from", "2026-06-01").param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0.00))
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void summaryRangeBoundsAreInclusive() throws Exception {
        createExpense("10.00", "2026-06-01", "FOOD"); // on the from boundary
        createExpense("20.00", "2026-06-30", "FOOD"); // on the to boundary
        createExpense("30.00", "2026-07-01", "FOOD"); // just outside

        mockMvc.perform(get("/api/summary").param("from", "2026-06-01").param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(30.00))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void summaryReturns400ForMalformedDateParam() throws Exception {
        mockMvc.perform(get("/api/summary").param("from", "2026-13-40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // --- Summary money / decimal precision -----------------------------------

    @Test
    void summaryTotalSumsManySmallAmountsExactlyWithoutFloatDrift() throws Exception {
        // 0.10 + 0.20 famously drifts in binary floating point; the server-side
        // aggregation must sum these exactly to 0.30.
        createExpense("0.10", "2026-06-10", "OTHER");
        createExpense("0.20", "2026-06-11", "OTHER");

        MvcResult result = mockMvc.perform(
                        get("/api/summary").param("from", "2026-06-01").param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = bigDecimalMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("total").decimalValue()).isEqualByComparingTo("0.30");
        assertThat(body.get("count").asLong()).isEqualTo(2L);
    }

    @Test
    void summaryTotalPreservesHighValueSumWithinNumeric12And2Range() throws Exception {
        createExpense("9999999998.99", "2026-06-10", "RENT");
        createExpense("1.00", "2026-06-11", "OTHER");

        MvcResult result = mockMvc.perform(
                        get("/api/summary").param("from", "2026-06-01").param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = bigDecimalMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("total").decimalValue()).isEqualByComparingTo("9999999999.99");
    }

    // --- GET /api/categories --------------------------------------------------

    @Test
    void categoriesReturnsFixedEnumSetInContractOrder() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(9)))
                .andExpect(jsonPath("$[0]").value("FOOD"))
                .andExpect(jsonPath("$[1]").value("TRANSPORT"))
                .andExpect(jsonPath("$[2]").value("RENT"))
                .andExpect(jsonPath("$[3]").value("UTILITIES"))
                .andExpect(jsonPath("$[4]").value("GROCERIES"))
                .andExpect(jsonPath("$[5]").value("ENTERTAINMENT"))
                .andExpect(jsonPath("$[6]").value("HEALTH"))
                .andExpect(jsonPath("$[7]").value("SHOPPING"))
                .andExpect(jsonPath("$[8]").value("OTHER"));
    }
}
