package com.expensetracker.repository;

import com.expensetracker.domain.Expense;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Persistence for {@link Expense}.
 *
 * <p>CRUD, sorting, and pagination come from {@link JpaRepository}. The list/
 * export endpoints (P3-1, P4-1) need an open-ended combination of optional
 * filters (date range, category, amount range), so we expose
 * {@link JpaSpecificationExecutor} and compose those filters with
 * {@link ExpenseSpecifications} rather than enumerating a derived-query method
 * per filter combination.
 */
public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {}
