package com.expensetracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies that Flyway's {@code V1__init.sql} applies cleanly against a real
 * Postgres (Testcontainers) and produces the schema the rest of Phase 1 relies
 * on: the {@code expenses} table with the expected columns/types, the two
 * indexes, and the {@code amount > 0} CHECK constraint.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class V1MigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsExpensesTableWithExpectedColumns() {
        Map<String, Map<String, Object>> columns = jdbc
                .queryForList(
                        """
                        SELECT column_name, data_type, is_nullable,
                               numeric_precision, numeric_scale, character_maximum_length
                        FROM information_schema.columns
                        WHERE table_name = 'expenses'
                        """)
                .stream()
                .collect(java.util.stream.Collectors.toMap(row -> (String) row.get("column_name"), row -> row));

        assertThat(columns.keySet())
                .containsExactlyInAnyOrder("id", "amount", "date", "category", "created_at", "updated_at");

        // Every column is NOT NULL.
        assertThat(columns.values())
                .allSatisfy(col -> assertThat(col.get("is_nullable")).isEqualTo("NO"));

        assertThat(columns.get("id")).containsEntry("data_type", "uuid");
        assertThat(columns.get("amount"))
                .containsEntry("data_type", "numeric")
                .containsEntry("numeric_precision", 12)
                .containsEntry("numeric_scale", 2);
        assertThat(columns.get("date")).containsEntry("data_type", "date");
        assertThat(columns.get("category"))
                .containsEntry("data_type", "character varying")
                .containsEntry("character_maximum_length", 32);
        assertThat(columns.get("created_at")).containsEntry("data_type", "timestamp with time zone");
        assertThat(columns.get("updated_at")).containsEntry("data_type", "timestamp with time zone");
    }

    @Test
    void createsIndexesOnDateAndCategory() {
        List<String> indexNames =
                jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE tablename = 'expenses'", String.class);

        assertThat(indexNames).contains("idx_expenses_date", "idx_expenses_category");
    }

    @Test
    void checkConstraintAcceptsPositiveAmountAndRejectsZeroOrNegative() {
        assertThat(insertAmount("0.01")).isEqualTo(1);

        assertThatThrownBy(() -> insertAmount("0.00")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAmount("-1.00")).isInstanceOf(DataIntegrityViolationException.class);
    }

    private int insertAmount(String amount) {
        return jdbc.update(
                """
                INSERT INTO expenses (amount, date, category, created_at, updated_at)
                VALUES (CAST(? AS NUMERIC), DATE '2026-06-10', 'OTHER', now(), now())
                """,
                amount);
    }
}
