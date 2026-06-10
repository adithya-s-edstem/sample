package com.expensetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.expensetracker.TestcontainersConfiguration;
import com.expensetracker.domain.Category;
import com.expensetracker.domain.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * Exercises {@link ExpenseRepository} and {@link ExpenseSpecifications} against a
 * real Postgres (Flyway-migrated schema + Testcontainers). Covers the
 * custom/composable filter queries (date range, category, amount range), their
 * combination, and the sorting/pagination the list endpoint (P3-1) relies on.
 *
 * <p>The boundary cases matter: the date and amount filters are inclusive, so
 * the seed data deliberately places rows exactly on each filter boundary.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ExpenseRepositoryIntegrationTest {

    @Autowired
    private ExpenseRepository repository;

    // Seed rows spanning May–July 2026, with values landing on the filter
    // boundaries (2026-06-01, 2026-06-30, amount 100.00, amount 200.00).
    private Expense may31Food100;
    private Expense jun01Groceries50;
    private Expense jun15Food200;
    private Expense jun30Transport001;
    private Expense jul01Rent1500;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        may31Food100 = new Expense(new BigDecimal("100.00"), LocalDate.of(2026, 5, 31), Category.FOOD);
        jun01Groceries50 = new Expense(new BigDecimal("50.00"), LocalDate.of(2026, 6, 1), Category.GROCERIES);
        jun15Food200 = new Expense(new BigDecimal("200.00"), LocalDate.of(2026, 6, 15), Category.FOOD);
        jun30Transport001 = new Expense(new BigDecimal("0.01"), LocalDate.of(2026, 6, 30), Category.TRANSPORT);
        jul01Rent1500 = new Expense(new BigDecimal("1500.00"), LocalDate.of(2026, 7, 1), Category.RENT);
        repository.saveAll(List.of(may31Food100, jun01Groceries50, jun15Food200, jun30Transport001, jul01Rent1500));
        repository.flush();
    }

    @Test
    void savesAndFindsById() {
        Expense found = repository.findById(jun15Food200.getId()).orElseThrow();

        assertThat(found.getAmount()).isEqualByComparingTo("200.00");
        assertThat(found.getCategory()).isEqualTo(Category.FOOD);
        assertThat(found.getDate()).isEqualTo(LocalDate.of(2026, 6, 15));
    }

    @Test
    void dateFromIsInclusive() {
        List<Expense> result = repository.findAll(ExpenseSpecifications.dateFrom(LocalDate.of(2026, 6, 1)));

        assertThat(result).containsExactlyInAnyOrder(jun01Groceries50, jun15Food200, jun30Transport001, jul01Rent1500);
    }

    @Test
    void dateToIsInclusive() {
        List<Expense> result = repository.findAll(ExpenseSpecifications.dateTo(LocalDate.of(2026, 6, 30)));

        assertThat(result).containsExactlyInAnyOrder(may31Food100, jun01Groceries50, jun15Food200, jun30Transport001);
    }

    @Test
    void dateRangeBracketsTheMonth() {
        Specification<Expense> june = Specification.allOf(
                ExpenseSpecifications.dateFrom(LocalDate.of(2026, 6, 1)),
                ExpenseSpecifications.dateTo(LocalDate.of(2026, 6, 30)));

        assertThat(repository.findAll(june))
                .containsExactlyInAnyOrder(jun01Groceries50, jun15Food200, jun30Transport001);
    }

    @Test
    void filtersByCategory() {
        assertThat(repository.findAll(ExpenseSpecifications.hasCategory(Category.FOOD)))
                .containsExactlyInAnyOrder(may31Food100, jun15Food200);
    }

    @Test
    void minAmountIsInclusive() {
        assertThat(repository.findAll(ExpenseSpecifications.minAmount(new BigDecimal("100.00"))))
                .containsExactlyInAnyOrder(may31Food100, jun15Food200, jul01Rent1500);
    }

    @Test
    void maxAmountIsInclusive() {
        assertThat(repository.findAll(ExpenseSpecifications.maxAmount(new BigDecimal("200.00"))))
                .containsExactlyInAnyOrder(may31Food100, jun01Groceries50, jun15Food200, jun30Transport001);
    }

    @Test
    void amountRangeBracketsValues() {
        Specification<Expense> range = Specification.allOf(
                ExpenseSpecifications.minAmount(new BigDecimal("50.00")),
                ExpenseSpecifications.maxAmount(new BigDecimal("200.00")));

        assertThat(repository.findAll(range)).containsExactlyInAnyOrder(jun01Groceries50, jun15Food200, may31Food100);
    }

    @Test
    void combinesEveryFilter() {
        Specification<Expense> spec = Specification.allOf(
                ExpenseSpecifications.dateFrom(LocalDate.of(2026, 6, 1)),
                ExpenseSpecifications.dateTo(LocalDate.of(2026, 6, 30)),
                ExpenseSpecifications.hasCategory(Category.FOOD),
                ExpenseSpecifications.minAmount(new BigDecimal("100.00")),
                ExpenseSpecifications.maxAmount(new BigDecimal("500.00")));

        // Only the 2026-06-15 FOOD/200.00 row satisfies all five constraints.
        assertThat(repository.findAll(spec)).containsExactly(jun15Food200);
    }

    @Test
    void nullSpecificationsImposeNoConstraint() {
        // Every factory returns null for a null argument; allOf treats null as
        // "no constraint", so an all-null spec must return every row.
        Specification<Expense> noFilters = Specification.allOf(
                ExpenseSpecifications.dateFrom(null),
                ExpenseSpecifications.dateTo(null),
                ExpenseSpecifications.hasCategory(null),
                ExpenseSpecifications.minAmount(null),
                ExpenseSpecifications.maxAmount(null));

        assertThat(repository.findAll(noFilters)).hasSize(5);
    }

    @Test
    void sortsByAmountAscending() {
        List<Expense> result = repository.findAll(Sort.by(Sort.Direction.ASC, "amount"));

        // 0.01, 50.00, 100.00, 200.00, 1500.00 — exact decimal ordering.
        assertThat(result)
                .containsExactly(jun30Transport001, jun01Groceries50, may31Food100, jun15Food200, jul01Rent1500);
    }

    @Test
    void sortsByDateDescending() {
        List<Expense> result = repository.findAll(Sort.by(Sort.Direction.DESC, "date"));

        assertThat(result)
                .containsExactly(jul01Rent1500, jun30Transport001, jun15Food200, jun01Groceries50, may31Food100);
    }

    @Test
    void paginatesSortedResults() {
        PageRequest firstPage = PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "date"));

        Page<Expense> page = repository.findAll(firstPage);

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getNumber()).isZero();
        assertThat(page.getContent()).containsExactly(may31Food100, jun01Groceries50);
    }

    @Test
    void paginatesWithFilterApplied() {
        Specification<Expense> june = Specification.allOf(
                ExpenseSpecifications.dateFrom(LocalDate.of(2026, 6, 1)),
                ExpenseSpecifications.dateTo(LocalDate.of(2026, 6, 30)));
        PageRequest secondPage = PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "date"));

        Page<Expense> page = repository.findAll(june, secondPage);

        // Three June rows, page size 2 → second page holds the trailing row.
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).containsExactly(jun30Transport001);
    }
}
