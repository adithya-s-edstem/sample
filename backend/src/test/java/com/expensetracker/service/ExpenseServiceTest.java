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
import com.expensetracker.web.dto.ExpenseRequest;
import com.expensetracker.web.dto.ExpenseResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
        when(repository.existsById(id)).thenReturn(true);

        service.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    void deleteThrowsAndDoesNotDeleteWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ExpenseNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }
}
