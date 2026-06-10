package com.expensetracker.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expensetracker.service.ExpenseService;
import com.expensetracker.web.dto.SummaryQuery;
import com.expensetracker.web.dto.SummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link SummaryController} with the service mocked. Verifies
 * the {@code GET /api/summary} HTTP contract from {@code docs/api-contracts.md}:
 * route, status, response body shape, and that the bound range params are passed
 * through to the service.
 */
@WebMvcTest(SummaryController.class)
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService service;

    @Test
    void returns200WithContractBodyAndDelegatesParsedRange() throws Exception {
        when(service.summary(any()))
                .thenReturn(SummaryResponse.of(
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), new BigDecimal("24500.00"), 42L));

        mockMvc.perform(get("/api/summary").param("from", "2026-06-01").param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-06-01"))
                .andExpect(jsonPath("$.to").value("2026-06-30"))
                .andExpect(jsonPath("$.total").value(24500.00))
                .andExpect(jsonPath("$.count").value(42))
                .andExpect(jsonPath("$.currency").value("INR"));

        ArgumentCaptor<SummaryQuery> captor = ArgumentCaptor.forClass(SummaryQuery.class);
        org.mockito.Mockito.verify(service).summary(captor.capture());
        assertThat(captor.getValue().from()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(captor.getValue().to()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void withNoParamsDelegatesNullRange() throws Exception {
        when(service.summary(any()))
                .thenReturn(SummaryResponse.of(
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), new BigDecimal("0.00"), 0L));

        mockMvc.perform(get("/api/summary")).andExpect(status().isOk());

        ArgumentCaptor<SummaryQuery> captor = ArgumentCaptor.forClass(SummaryQuery.class);
        org.mockito.Mockito.verify(service).summary(captor.capture());
        assertThat(captor.getValue().from()).isNull();
        assertThat(captor.getValue().to()).isNull();
    }

    @Test
    void returns400ForMalformedDateParam() throws Exception {
        mockMvc.perform(get("/api/summary").param("from", "2026-13-40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
