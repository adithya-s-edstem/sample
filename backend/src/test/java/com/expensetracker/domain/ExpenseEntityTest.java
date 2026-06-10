package com.expensetracker.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.expensetracker.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Verifies the {@link Expense} ↔ {@code expenses} mapping round-trips against a
 * real Postgres (via Flyway-migrated schema + Testcontainers). ddl-auto=validate
 * means this also proves the entity matches {@code V1__init.sql}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ExpenseEntityTest {

    @Autowired
    private TestEntityManager em;

    @Test
    void persistsAndReloadsWithGeneratedIdAndTimestamps() {
        Expense expense = new Expense(new BigDecimal("1200.00"), LocalDate.of(2026, 6, 10), Category.GROCERIES);

        Expense saved = em.persistFlushFind(expense);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCategory()).isEqualTo(Category.GROCERIES);
        assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    void preservesExactDecimalScale() {
        Expense expense = new Expense(new BigDecimal("0.01"), LocalDate.of(2026, 6, 10), Category.OTHER);

        Expense saved = em.persistFlushFind(expense);
        em.clear();
        Expense reloaded = em.find(Expense.class, saved.getId());

        // NUMERIC(12,2) round-trips exactly, with scale preserved (no float drift).
        assertThat(reloaded.getAmount()).isEqualByComparingTo("0.01");
        assertThat(reloaded.getAmount().scale()).isEqualTo(2);
    }

    @Test
    void categoryIsStoredAsItsEnumName() {
        Expense expense = new Expense(new BigDecimal("50.00"), LocalDate.of(2026, 6, 10), Category.TRANSPORT);
        Expense saved = em.persistFlushFind(expense);

        String stored = (String) em.getEntityManager()
                .createNativeQuery("SELECT category FROM expenses WHERE id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();

        assertThat(stored).isEqualTo("TRANSPORT");
    }
}
