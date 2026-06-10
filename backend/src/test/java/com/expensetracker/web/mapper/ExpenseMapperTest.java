package com.expensetracker.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.expensetracker.domain.Category;
import com.expensetracker.domain.Expense;
import com.expensetracker.web.dto.ExpenseRequest;
import com.expensetracker.web.dto.ExpenseResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link ExpenseMapper}. Pure mapping logic, no Spring context.
 * {@code id}/{@code createdAt}/{@code updatedAt} are normally set by JPA, so the
 * {@code toResponse} test seeds them reflectively to prove they are carried
 * through.
 */
class ExpenseMapperTest {

    @Test
    void toEntityCopiesClientFieldsAndLeavesJpaFieldsUnset() {
        ExpenseRequest request =
                new ExpenseRequest(new BigDecimal("1200.00"), LocalDate.of(2026, 6, 10), Category.GROCERIES);

        Expense entity = ExpenseMapper.toEntity(request);

        assertThat(entity.getAmount()).isEqualByComparingTo("1200.00");
        assertThat(entity.getDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(entity.getCategory()).isEqualTo(Category.GROCERIES);
        // id/timestamps are owned by JPA, not the mapper.
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void toEntityPreservesExactDecimalScale() {
        ExpenseRequest request = new ExpenseRequest(new BigDecimal("0.01"), LocalDate.of(2026, 6, 10), Category.OTHER);

        Expense entity = ExpenseMapper.toEntity(request);

        assertThat(entity.getAmount()).isEqualByComparingTo("0.01");
        assertThat(entity.getAmount().scale()).isEqualTo(2);
    }

    @Test
    void applyToOverwritesMutableFieldsOnly() {
        Expense existing = new Expense(new BigDecimal("100.00"), LocalDate.of(2026, 5, 1), Category.FOOD);
        UUID id = UUID.randomUUID();
        Instant created = Instant.parse("2026-05-01T08:00:00Z");
        ReflectionTestUtils.setField(existing, "id", id);
        ReflectionTestUtils.setField(existing, "createdAt", created);

        ExpenseRequest update = new ExpenseRequest(new BigDecimal("250.50"), LocalDate.of(2026, 6, 2), Category.HEALTH);
        ExpenseMapper.applyTo(update, existing);

        assertThat(existing.getAmount()).isEqualByComparingTo("250.50");
        assertThat(existing.getDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(existing.getCategory()).isEqualTo(Category.HEALTH);
        // Identity and creation timestamp are untouched by an update mapping.
        assertThat(existing.getId()).isEqualTo(id);
        assertThat(existing.getCreatedAt()).isEqualTo(created);
    }

    @Test
    void toResponseProjectsEveryField() {
        Expense entity = new Expense(new BigDecimal("999.99"), LocalDate.of(2026, 6, 10), Category.RENT);
        UUID id = UUID.randomUUID();
        Instant created = Instant.parse("2026-06-10T09:15:30Z");
        Instant updated = Instant.parse("2026-06-10T10:20:00Z");
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "createdAt", created);
        ReflectionTestUtils.setField(entity, "updatedAt", updated);

        ExpenseResponse response = ExpenseMapper.toResponse(entity);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.amount()).isEqualByComparingTo("999.99");
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(response.category()).isEqualTo(Category.RENT);
        assertThat(response.createdAt()).isEqualTo(created);
        assertThat(response.updatedAt()).isEqualTo(updated);
    }
}
