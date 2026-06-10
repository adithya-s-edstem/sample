package com.expensetracker.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expensetracker.domain.Category;
import com.expensetracker.service.ExpenseNotFoundException;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.web.csv.ExpenseCsvWriter;
import com.expensetracker.web.dto.ExpenseQuery;
import com.expensetracker.web.dto.ExpenseRequest;
import com.expensetracker.web.dto.ExpenseResponse;
import com.expensetracker.web.dto.PageResponse;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
    void listReturnsPagedBodyAndDelegatesParsedQuery() throws Exception {
        ExpenseResponse row = sampleResponse(new BigDecimal("1200.00"), LocalDate.of(2026, 6, 10), Category.GROCERIES);
        when(service.list(any())).thenReturn(new PageResponse<>(List.of(row), 0, 50, 1L, 1, "date,desc"));

        mockMvc.perform(get("/api/expenses")
                        .param("category", "GROCERIES")
                        .param("minAmount", "100")
                        .param("sort", "amount,asc")
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(ID.toString()))
                .andExpect(jsonPath("$.content[0].amount").value(1200.00))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.sort").value("date,desc"));

        ArgumentCaptor<ExpenseQuery> captor = ArgumentCaptor.forClass(ExpenseQuery.class);
        verify(service).list(captor.capture());
        ExpenseQuery q = captor.getValue();
        assertThat(q.category()).isEqualTo(Category.GROCERIES);
        assertThat(q.minAmount()).isEqualByComparingTo("100");
        assertThat(q.sort()).isEqualTo("amount,asc");
        assertThat(q.page()).isZero();
        assertThat(q.size()).isEqualTo(50);
    }

    @Test
    void listWithNoParamsDelegatesAllNullQuery() throws Exception {
        when(service.list(any())).thenReturn(new PageResponse<>(List.of(), 0, 50, 0L, 0, "date,desc"));

        mockMvc.perform(get("/api/expenses")).andExpect(status().isOk());

        ArgumentCaptor<ExpenseQuery> captor = ArgumentCaptor.forClass(ExpenseQuery.class);
        verify(service).list(captor.capture());
        ExpenseQuery q = captor.getValue();
        assertThat(q.from()).isNull();
        assertThat(q.to()).isNull();
        assertThat(q.category()).isNull();
        assertThat(q.sort()).isNull();
    }

    @Test
    void listReturns400ForMalformedDateParam() throws Exception {
        mockMvc.perform(get("/api/expenses").param("from", "2026-13-40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listReturns400ForUnknownCategoryParam() throws Exception {
        mockMvc.perform(get("/api/expenses").param("category", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void exportStreamsCsvWithHeadersAndDelegatesParsedQuery() throws Exception {
        // The streaming endpoint writes via service.exportCsv(query, writer); stub it
        // to render one row through the real writer so we assert the wire body too.
        ExpenseResponse row = sampleResponse(new BigDecimal("1200.00"), LocalDate.of(2026, 6, 10), Category.GROCERIES);
        doAnswer(invocation -> {
                    Writer writer = invocation.getArgument(1);
                    ExpenseCsvWriter.writeTo(writer, java.util.stream.Stream.of(row));
                    return null;
                })
                .when(service)
                .exportCsv(any(), any());

        MvcResult started = mockMvc.perform(get("/api/expenses/export")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30")
                        .param("category", "GROCERIES")
                        .param("minAmount", "100"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(
                                "Content-Disposition",
                                org.hamcrest.Matchers.startsWith("attachment; filename=\"expenses-")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.endsWith(".csv\"")))
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("id,date,category,amount")))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString(ID + ",2026-06-10,GROCERIES,1200.00")));

        ArgumentCaptor<ExpenseQuery> captor = ArgumentCaptor.forClass(ExpenseQuery.class);
        verify(service).exportCsv(captor.capture(), any());
        ExpenseQuery q = captor.getValue();
        assertThat(q.from()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(q.to()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(q.category()).isEqualTo(Category.GROCERIES);
        assertThat(q.minAmount()).isEqualByComparingTo("100");
        // Export never paginates — page/size are not bound.
        assertThat(q.page()).isNull();
        assertThat(q.size()).isNull();
    }

    @Test
    void exportReturns400ForMalformedDateParam() throws Exception {
        // Binding fails before the async stream starts, so the body is never invoked.
        mockMvc.perform(get("/api/expenses/export").param("from", "2026-13-40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(service, never()).exportCsv(any(), any());
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

    @Test
    void putReturns404WhenServiceReportsMissing() throws Exception {
        when(service.update(eq(ID), any())).thenThrow(new ExpenseNotFoundException(ID));

        mockMvc.perform(put("/api/expenses/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"date\":\"2026-06-10\",\"category\":\"FOOD\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void putReturns400AndDoesNotReachServiceForInvalidBody() throws Exception {
        // amount <= 0 fails @Valid before the controller delegates to the service.
        mockMvc.perform(put("/api/expenses/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0,\"date\":\"2026-06-10\",\"category\":\"FOOD\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).update(any(), any());
    }

    @Test
    void deleteReturns404WhenServiceReportsMissing() throws Exception {
        doThrow(new ExpenseNotFoundException(ID)).when(service).delete(ID);

        mockMvc.perform(delete("/api/expenses/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
