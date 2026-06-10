package com.expensetracker.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expensetracker.domain.Category;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.web.dto.ExpenseRequest;
import com.expensetracker.web.dto.ExpenseResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link ExpenseController} with the service mocked. Verifies
 * the HTTP contract from {@code docs/api-contracts.md}: routes, status codes,
 * request deserialization, and response body shape. Validation/error paths
 * (400/404) arrive with the P2-4 handler.
 */
@WebMvcTest(ExpenseController.class)
class ExpenseControllerTest {

    private static final UUID ID = UUID.fromString("9f1c2e7a-3b6d-4f2a-8c11-2a7e9d0b4c55");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService service;

    private static ExpenseResponse sampleResponse(BigDecimal amount, LocalDate date, Category category) {
        return new ExpenseResponse(
                ID,
                amount,
                date,
                category,
                Instant.parse("2026-06-10T09:15:30Z"),
                Instant.parse("2026-06-10T09:15:30Z"));
    }

    @Test
    void createReturns201WithBodyAndDelegatesParsedRequest() throws Exception {
        when(service.create(any()))
                .thenReturn(sampleResponse(new BigDecimal("1200.00"), LocalDate.of(2026, 6, 10), Category.GROCERIES));

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1200.00,\"date\":\"2026-06-10\",\"category\":\"GROCERIES\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.amount").value(1200.00))
                .andExpect(jsonPath("$.date").value("2026-06-10"))
                .andExpect(jsonPath("$.category").value("GROCERIES"))
                .andExpect(jsonPath("$.createdAt").value("2026-06-10T09:15:30Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-06-10T09:15:30Z"));

        ArgumentCaptor<ExpenseRequest> captor = ArgumentCaptor.forClass(ExpenseRequest.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().amount()).isEqualByComparingTo("1200.00");
        assertThat(captor.getValue().date()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(captor.getValue().category()).isEqualTo(Category.GROCERIES);
    }

    @Test
    void getReturns200WithBody() throws Exception {
        when(service.get(ID))
                .thenReturn(sampleResponse(new BigDecimal("50.00"), LocalDate.of(2026, 6, 1), Category.FOOD));

        mockMvc.perform(get("/api/expenses/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.date").value("2026-06-01"))
                .andExpect(jsonPath("$.category").value("FOOD"));
    }

    @Test
    void putReturns200WithUpdatedBodyAndDelegates() throws Exception {
        when(service.update(eq(ID), any()))
                .thenReturn(sampleResponse(new BigDecimal("250.50"), LocalDate.of(2026, 6, 2), Category.HEALTH));

        mockMvc.perform(put("/api/expenses/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":250.50,\"date\":\"2026-06-02\",\"category\":\"HEALTH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.amount").value(250.50))
                .andExpect(jsonPath("$.date").value("2026-06-02"))
                .andExpect(jsonPath("$.category").value("HEALTH"));

        ArgumentCaptor<ExpenseRequest> captor = ArgumentCaptor.forClass(ExpenseRequest.class);
        verify(service).update(eq(ID), captor.capture());
        assertThat(captor.getValue().amount()).isEqualByComparingTo("250.50");
        assertThat(captor.getValue().category()).isEqualTo(Category.HEALTH);
    }

    @Test
    void deleteReturns204AndDelegates() throws Exception {
        mockMvc.perform(delete("/api/expenses/{id}", ID)).andExpect(status().isNoContent());

        verify(service).delete(ID);
    }
}
