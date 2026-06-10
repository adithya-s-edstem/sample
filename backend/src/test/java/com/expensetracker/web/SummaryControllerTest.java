package com.expensetracker.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expensetracker.domain.Category;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.web.dto.CategorySummaryResponse;
import com.expensetracker.web.dto.CategorySummaryResponse.CategoryShare;
import com.expensetracker.web.dto.Granularity;
import com.expensetracker.web.dto.SummaryQuery;
import com.expensetracker.web.dto.SummaryResponse;
import com.expensetracker.web.dto.TrendQuery;
import com.expensetracker.web.dto.TrendResponse;
import com.expensetracker.web.dto.TrendResponse.TrendPoint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    // --- GET /api/summary/by-category -----------------------------------------

    @Test
    void byCategoryReturns200WithContractBodyAndDelegatesParsedRange() throws Exception {
        when(service.summaryByCategory(any()))
                .thenReturn(new CategorySummaryResponse(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30),
                        new BigDecimal("24500.00"),
                        List.of(
                                new CategoryShare(
                                        Category.RENT, new BigDecimal("12000.00"), 1L, new BigDecimal("48.98")),
                                new CategoryShare(
                                        Category.GROCERIES, new BigDecimal("5200.00"), 8L, new BigDecimal("21.22")))));

        mockMvc.perform(get("/api/summary/by-category")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-06-01"))
                .andExpect(jsonPath("$.to").value("2026-06-30"))
                .andExpect(jsonPath("$.total").value(24500.00))
                .andExpect(jsonPath("$.categories", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.categories[0].category").value("RENT"))
                .andExpect(jsonPath("$.categories[0].total").value(12000.00))
                .andExpect(jsonPath("$.categories[0].count").value(1))
                .andExpect(jsonPath("$.categories[0].percent").value(48.98))
                .andExpect(jsonPath("$.categories[1].category").value("GROCERIES"))
                .andExpect(jsonPath("$.categories[1].percent").value(21.22));

        ArgumentCaptor<SummaryQuery> captor = ArgumentCaptor.forClass(SummaryQuery.class);
        org.mockito.Mockito.verify(service).summaryByCategory(captor.capture());
        assertThat(captor.getValue().from()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(captor.getValue().to()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void byCategoryWithNoParamsDelegatesNullRange() throws Exception {
        when(service.summaryByCategory(any()))
                .thenReturn(new CategorySummaryResponse(
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), new BigDecimal("0.00"), List.of()));

        mockMvc.perform(get("/api/summary/by-category")).andExpect(status().isOk());

        ArgumentCaptor<SummaryQuery> captor = ArgumentCaptor.forClass(SummaryQuery.class);
        org.mockito.Mockito.verify(service).summaryByCategory(captor.capture());
        assertThat(captor.getValue().from()).isNull();
        assertThat(captor.getValue().to()).isNull();
    }

    @Test
    void byCategoryReturns400ForMalformedDateParam() throws Exception {
        mockMvc.perform(get("/api/summary/by-category").param("from", "2026-13-40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // --- GET /api/summary/trend ------------------------------------------------

    @Test
    void trendReturns200WithContractBodyAndDelegatesParsedRangeAndGranularity() throws Exception {
        when(service.summaryTrend(any()))
                .thenReturn(new TrendResponse(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 6, 30),
                        Granularity.MONTH,
                        List.of(
                                new TrendPoint("2026-01", new BigDecimal("21000.00")),
                                new TrendPoint("2026-02", new BigDecimal("18750.00")))));

        mockMvc.perform(get("/api/summary/trend")
                        .param("from", "2026-01-01")
                        .param("to", "2026-06-30")
                        .param("granularity", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-01-01"))
                .andExpect(jsonPath("$.to").value("2026-06-30"))
                // granularity is serialized lowercase on the wire.
                .andExpect(jsonPath("$.granularity").value("month"))
                .andExpect(jsonPath("$.points", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.points[0].period").value("2026-01"))
                .andExpect(jsonPath("$.points[0].total").value(21000.00))
                .andExpect(jsonPath("$.points[1].period").value("2026-02"))
                .andExpect(jsonPath("$.points[1].total").value(18750.00));

        ArgumentCaptor<TrendQuery> captor = ArgumentCaptor.forClass(TrendQuery.class);
        org.mockito.Mockito.verify(service).summaryTrend(captor.capture());
        assertThat(captor.getValue().from()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(captor.getValue().to()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(captor.getValue().granularity()).isEqualTo(Granularity.MONTH);
    }

    @Test
    void trendAcceptsGranularityCaseInsensitively() throws Exception {
        when(service.summaryTrend(any()))
                .thenReturn(new TrendResponse(
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), Granularity.DAY, List.of()));

        mockMvc.perform(get("/api/summary/trend").param("granularity", "DAY")).andExpect(status().isOk());

        ArgumentCaptor<TrendQuery> captor = ArgumentCaptor.forClass(TrendQuery.class);
        org.mockito.Mockito.verify(service).summaryTrend(captor.capture());
        assertThat(captor.getValue().granularity()).isEqualTo(Granularity.DAY);
    }

    @Test
    void trendWithNoParamsDelegatesNullRangeAndNullGranularity() throws Exception {
        when(service.summaryTrend(any()))
                .thenReturn(new TrendResponse(
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), Granularity.DAY, List.of()));

        mockMvc.perform(get("/api/summary/trend")).andExpect(status().isOk());

        ArgumentCaptor<TrendQuery> captor = ArgumentCaptor.forClass(TrendQuery.class);
        org.mockito.Mockito.verify(service).summaryTrend(captor.capture());
        assertThat(captor.getValue().from()).isNull();
        assertThat(captor.getValue().to()).isNull();
        assertThat(captor.getValue().granularity()).isNull();
    }

    @Test
    void trendReturns400ForMalformedDateParam() throws Exception {
        mockMvc.perform(get("/api/summary/trend").param("from", "2026-13-40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void trendReturns400ForUnknownGranularity() throws Exception {
        mockMvc.perform(get("/api/summary/trend").param("granularity", "weekly"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
