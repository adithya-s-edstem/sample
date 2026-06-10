package com.expensetracker.web.error;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expensetracker.domain.Category;
import com.expensetracker.service.ExpenseNotFoundException;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.web.ExpenseController;
import com.expensetracker.web.dto.ExpenseResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies {@link GlobalExceptionHandler} + the {@code @Valid} constraints on
 * {@link com.expensetracker.web.dto.ExpenseRequest} produce the uniform error
 * shape from {@code docs/api-contracts.md}: validation → 400 with
 * {@code fieldErrors[]}, missing/unknown id → 404, malformed body/params → 400.
 *
 * <p>The {@code @WebMvcTest} slice auto-detects the {@code @RestControllerAdvice}.
 */
@WebMvcTest(ExpenseController.class)
class GlobalExceptionHandlerTest {

    private static final UUID ID = UUID.fromString("9f1c2e7a-3b6d-4f2a-8c11-2a7e9d0b4c55");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService service;

    private static String body(String amount, String date, String category) {
        return "{\"amount\":%s,\"date\":%s,\"category\":%s}".formatted(amount, date, category);
    }

    @Test
    void rejectsZeroAmountWithUniformErrorShape() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("0", "\"2026-06-10\"", "\"FOOD\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/expenses"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must be greater than 0"));
    }

    @Test
    void rejectsNegativeAmount() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("-1.00", "\"2026-06-10\"", "\"FOOD\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
    }

    @Test
    void acceptsSmallestPositiveAmount() throws Exception {
        when(service.create(any()))
                .thenReturn(new ExpenseResponse(
                        ID,
                        new BigDecimal("0.01"),
                        LocalDate.of(2026, 6, 10),
                        Category.OTHER,
                        Instant.parse("2026-06-10T09:15:30Z"),
                        Instant.parse("2026-06-10T09:15:30Z")));

        // 0.01 is the boundary of "> 0" — it must pass validation and reach the service.
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("0.01", "\"2026-06-10\"", "\"OTHER\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(0.01));
    }

    @Test
    void rejectsMissingFieldsWithOneErrorPerField() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("null", "null", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(3)))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("amount", "date", "category")));
    }

    @Test
    void rejectsUnknownCategory() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("10.00", "\"2026-06-10\"", "\"NOT_A_CATEGORY\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsMalformedDate() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("10.00", "\"2026-13-40\"", "\"FOOD\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404WithUniformShapeWhenExpenseMissing() throws Exception {
        when(service.get(ID)).thenThrow(new ExpenseNotFoundException(ID));

        mockMvc.perform(get("/api/expenses/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/expenses/" + ID))
                .andExpect(jsonPath("$.message").value("Expense not found: " + ID))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void returns400ForMalformedUuidPath() throws Exception {
        mockMvc.perform(get("/api/expenses/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
