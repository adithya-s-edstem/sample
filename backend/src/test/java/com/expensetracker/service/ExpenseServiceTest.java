package com.expensetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expensetracker.domain.Category;
import com.expensetracker.domain.Expense;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.projection.CategorySummaryProjection;
import com.expensetracker.repository.projection.SummaryProjection;
import com.expensetracker.web.dto.CategorySummaryResponse;
import com.expensetracker.web.dto.ExpenseRequest;
import com.expensetracker.web.dto.ExpenseResponse;
import com.expensetracker.web.dto.SummaryQuery;
import com.expensetracker.web.dto.SummaryResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link ExpenseService} with the repository mocked — pure
 * business logic (mapping wiring, not-found signalling), no database.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository repository;

    @InjectMocks
    private ExpenseService service;

    private static Expense persisted(UUID id, BigDecimal amount, LocalDate date, Category category) {
        Expense expense = new Expense(amount, date, category);
        ReflectionTestUtils.setField(expense, "id", id);
        ReflectionTestUtils.setField(expense, "createdAt", Instant.parse("2026-06-10T09:00:00Z"));
        ReflectionTestUtils.setField(expense, "updatedAt", Instant.parse("2026-06-10T09:00:00Z"));
        return expense;
    }

    @Test
    void createPersistsMappedEntityAndReturnsResponse() {
        UUID id = UUID.randomUUID();
        when(repository.save(any(Expense.class))).thenAnswer(inv -> {
            Expense arg = inv.getArgument(0);
            ReflectionTestUtils.setField(arg, "id", id);
            ReflectionTestUtils.setField(arg, "createdAt", Instant.parse("2026-06-10T09:00:00Z"));
            ReflectionTestUtils.setField(arg, "updatedAt", Instant.parse("2026-06-10T09:00:00Z"));
            return arg;
        });
        ExpenseRequest request =
                new ExpenseRequest(new BigDecimal("1200.00"), LocalDate.of(2026, 6, 10), Category.GROCERIES);

        ExpenseResponse response = service.create(request);

        ArgumentCaptor<Expense> saved = ArgumentCaptor.forClass(Expense.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("1200.00");
        assertThat(saved.getValue().getCategory()).isEqualTo(Category.GROCERIES);
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.amount()).isEqualByComparingTo("1200.00");
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(response.category()).isEqualTo(Category.GROCERIES);
    }

    @Test
    void getReturnsResponseWhenFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id))
                .thenReturn(
                        Optional.of(persisted(id, new BigDecimal("50.00"), LocalDate.of(2026, 6, 1), Category.FOOD)));

        ExpenseResponse response = service.get(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.amount()).isEqualByComparingTo("50.00");
        assertThat(response.category()).isEqualTo(Category.FOOD);
    }

    @Test
    void getMapsTimestampsIntoResponse() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id))
                .thenReturn(
                        Optional.of(persisted(id, new BigDecimal("50.00"), LocalDate.of(2026, 6, 1), Category.FOOD)));

        ExpenseResponse response = service.get(id);

        // createdAt/updatedAt must survive the entity→response mapping unchanged.
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-06-10T09:00:00Z"));
        assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-06-10T09:00:00Z"));
    }

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(ExpenseNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateAppliesRequestFieldsAndPersists() {
        UUID id = UUID.randomUUID();
        Expense existing = persisted(id, new BigDecimal("100.00"), LocalDate.of(2026, 5, 1), Category.FOOD);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        ExpenseRequest request =
                new ExpenseRequest(new BigDecimal("250.50"), LocalDate.of(2026, 6, 2), Category.HEALTH);

        ExpenseResponse response = service.update(id, request);

        // The same managed entity is mutated and re-saved, keeping its identity.
        ArgumentCaptor<Expense> saved = ArgumentCaptor.forClass(Expense.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(id);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("250.50");
        assertThat(saved.getValue().getDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(saved.getValue().getCategory()).isEqualTo(Category.HEALTH);
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.amount()).isEqualByComparingTo("250.50");
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(response.category()).isEqualTo(Category.HEALTH);
    }

    @Test
    void updateThrowsAndDoesNotSaveWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        ExpenseRequest request = new ExpenseRequest(new BigDecimal("10.00"), LocalDate.of(2026, 6, 2), Category.OTHER);

        assertThatThrownBy(() -> service.update(id, request)).isInstanceOf(ExpenseNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteRemovesWhenExists() {
        UUID id = UUID.randomUUID();
        Expense existing = persisted(id, new BigDecimal("75.00"), LocalDate.of(2026, 6, 5), Category.SHOPPING);
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        service.delete(id);

        verify(repository).delete(existing);
    }

    @Test
    void deleteThrowsAndDoesNotDeleteWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ExpenseNotFoundException.class);
        verify(repository, never()).delete(any(Expense.class));
    }

    // --- summary (GET /api/summary) -------------------------------------------

    private static SummaryProjection projection(BigDecimal total, long count) {
        return new SummaryProjection() {
            @Override
            public BigDecimal getTotal() {
                return total;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    @Test
    void summaryAggregatesTotalCountAndEchoesExplicitRange() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(repository.summarize(from, to)).thenReturn(projection(new BigDecimal("24500.00"), 42L));

        SummaryResponse response = service.summary(new SummaryQuery(from, to));

        assertThat(response.from()).isEqualTo(from);
        assertThat(response.to()).isEqualTo(to);
        assertThat(response.total()).isEqualByComparingTo("24500.00");
        assertThat(response.count()).isEqualTo(42L);
        assertThat(response.currency()).isEqualTo("INR");
    }

    @Test
    void summaryDefaultsRangeToCurrentMonthWhenOmitted() {
        YearMonth thisMonth = YearMonth.now();
        LocalDate first = thisMonth.atDay(1);
        LocalDate last = thisMonth.atEndOfMonth();
        when(repository.summarize(first, last)).thenReturn(projection(new BigDecimal("100.00"), 3L));

        SummaryResponse response = service.summary(new SummaryQuery(null, null));

        // The current-month bounds are passed to the repository and echoed back.
        verify(repository).summarize(first, last);
        assertThat(response.from()).isEqualTo(first);
        assertThat(response.to()).isEqualTo(last);
    }

    @Test
    void summaryReportsZeroTotalWithTwoDecimalScaleForEmptyPeriod() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        // The query coalesces SUM(null) to 0; the service normalizes it to scale 2.
        when(repository.summarize(from, to)).thenReturn(projection(BigDecimal.ZERO, 0L));

        SummaryResponse response = service.summary(new SummaryQuery(from, to));

        assertThat(response.total()).isEqualByComparingTo("0.00");
        assertThat(response.total().scale()).isEqualTo(2);
        assertThat(response.count()).isZero();
    }

    // --- summaryByCategory (GET /api/summary/by-category) ----------------------

    private static CategorySummaryProjection categoryRow(Category category, BigDecimal total, long count) {
        return new CategorySummaryProjection() {
            @Override
            public Category getCategory() {
                return category;
            }

            @Override
            public BigDecimal getTotal() {
                return total;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    @Test
    void byCategoryComputesGrandTotalSlicesAndPercentsFromExactTotals() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(repository.summarizeByCategory(from, to))
                .thenReturn(List.of(
                        categoryRow(Category.RENT, new BigDecimal("12000.00"), 1L),
                        categoryRow(Category.GROCERIES, new BigDecimal("5200.00"), 8L),
                        categoryRow(Category.FOOD, new BigDecimal("3300.00"), 12L)));

        CategorySummaryResponse response = service.summaryByCategory(new SummaryQuery(from, to));

        assertThat(response.from()).isEqualTo(from);
        assertThat(response.to()).isEqualTo(to);
        assertThat(response.total()).isEqualByComparingTo("20500.00");
        assertThat(response.categories()).hasSize(3);
        // Percent share is each slice / grand total, half-up to 2 decimals.
        assertThat(response.categories().get(0).category()).isEqualTo(Category.RENT);
        assertThat(response.categories().get(0).total()).isEqualByComparingTo("12000.00");
        assertThat(response.categories().get(0).count()).isEqualTo(1L);
        assertThat(response.categories().get(0).percent()).isEqualByComparingTo("58.54");
        assertThat(response.categories().get(1).percent()).isEqualByComparingTo("25.37");
        assertThat(response.categories().get(2).percent()).isEqualByComparingTo("16.10");
    }

    @Test
    void byCategoryDefaultsRangeToCurrentMonthWhenOmitted() {
        YearMonth thisMonth = YearMonth.now();
        LocalDate first = thisMonth.atDay(1);
        LocalDate last = thisMonth.atEndOfMonth();
        when(repository.summarizeByCategory(first, last))
                .thenReturn(List.of(categoryRow(Category.FOOD, new BigDecimal("100.00"), 2L)));

        CategorySummaryResponse response = service.summaryByCategory(new SummaryQuery(null, null));

        verify(repository).summarizeByCategory(first, last);
        assertThat(response.from()).isEqualTo(first);
        assertThat(response.to()).isEqualTo(last);
    }

    @Test
    void byCategoryReportsZeroTotalAndEmptyListForEmptyPeriod() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(repository.summarizeByCategory(from, to)).thenReturn(List.of());

        CategorySummaryResponse response = service.summaryByCategory(new SummaryQuery(from, to));

        // No rows → grand total 0.00 (scale 2), no slices, and no divide-by-zero.
        assertThat(response.total()).isEqualByComparingTo("0.00");
        assertThat(response.total().scale()).isEqualTo(2);
        assertThat(response.categories()).isEmpty();
    }

    @Test
    void byCategorySingleCategoryGetsHundredPercent() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(repository.summarizeByCategory(from, to))
                .thenReturn(List.of(categoryRow(Category.RENT, new BigDecimal("12000.00"), 1L)));

        CategorySummaryResponse response = service.summaryByCategory(new SummaryQuery(from, to));

        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().get(0).percent()).isEqualByComparingTo("100.00");
    }
}
