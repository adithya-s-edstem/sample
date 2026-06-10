package com.expensetracker.web.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.expensetracker.domain.Category;
import com.expensetracker.web.dto.ExpenseResponse;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ExpenseCsvWriter} (P4-1/P4-2) — the CSV body shape from
 * section 4 of {@code docs/api-contracts.md}, the money-precision rules (two
 * decimals, never scientific notation) from {@code CLAUDE.md}, and the streaming
 * {@code writeTo} path that backs the export endpoint (P4-2).
 */
class ExpenseCsvWriterTest {

    private static final UUID ID = UUID.fromString("9f1c2e7a-3b6d-4f2a-8c11-2a7e9d0b4c55");

    private static ExpenseResponse row(String amount, LocalDate date, Category category) {
        return new ExpenseResponse(
                ID, new BigDecimal(amount), date, category, Instant.parse("2026-06-10T09:15:30Z"), null);
    }

    @Test
    void writesHeaderOnlyForNoRows() {
        String csv = ExpenseCsvWriter.write(List.of());

        assertThat(csv).isEqualTo("id,date,category,amount\r\n");
    }

    @Test
    void writesHeaderThenRowMatchingContractColumns() {
        String csv = ExpenseCsvWriter.write(List.of(row("1200.00", LocalDate.of(2026, 6, 10), Category.GROCERIES)));

        assertThat(csv)
                .isEqualTo("id,date,category,amount\r\n"
                        + "9f1c2e7a-3b6d-4f2a-8c11-2a7e9d0b4c55,2026-06-10,GROCERIES,1200.00\r\n");
    }

    @Test
    void writesAmountsWithExactlyTwoDecimals() {
        // A whole-rupee amount and a sub-rupee amount both render at scale 2.
        String csv = ExpenseCsvWriter.write(List.of(
                row("300", LocalDate.of(2026, 6, 9), Category.TRANSPORT),
                row("0.01", LocalDate.of(2026, 6, 8), Category.FOOD)));

        assertThat(csv).contains(",300.00\r\n").contains(",0.01\r\n");
    }

    @Test
    void rendersHighValueAmountsInPlainNotationNotScientific() {
        // Top of NUMERIC(12,2) range — must stay plain decimal, never 1.0E9.
        String csv = ExpenseCsvWriter.write(List.of(row("9999999999.99", LocalDate.of(2026, 6, 1), Category.RENT)));

        // The amount field is plain decimal: full digits, no exponent marker.
        String amountField = csv.split("\r\n")[1].split(",")[3];
        assertThat(amountField).isEqualTo("9999999999.99").doesNotContainIgnoringCase("e");
    }

    @Test
    void preservesRowOrderAsGiven() {
        String csv = ExpenseCsvWriter.write(List.of(
                row("10.00", LocalDate.of(2026, 6, 3), Category.FOOD),
                row("20.00", LocalDate.of(2026, 6, 1), Category.RENT)));

        String[] lines = csv.split("\r\n");
        assertThat(lines).hasSize(3);
        assertThat(lines[1]).endsWith("2026-06-03,FOOD,10.00");
        assertThat(lines[2]).endsWith("2026-06-01,RENT,20.00");
    }

    // --- streaming writeTo (P4-2) ---------------------------------------------

    @Test
    void streamingWritesHeaderOnlyForEmptyStream() {
        StringWriter out = new StringWriter();

        ExpenseCsvWriter.writeTo(out, Stream.of());

        assertThat(out.toString()).isEqualTo("id,date,category,amount\r\n");
    }

    @Test
    void streamingProducesIdenticalOutputToInMemoryWrite() {
        // The streaming path and the materialized path must render byte-for-byte the
        // same CSV, so the endpoint's switch to streaming changes nothing on the wire.
        List<ExpenseResponse> rows = List.of(
                row("1200.00", LocalDate.of(2026, 6, 10), Category.GROCERIES),
                row("0.01", LocalDate.of(2026, 6, 8), Category.FOOD),
                row("9999999999.99", LocalDate.of(2026, 6, 1), Category.RENT));

        StringWriter out = new StringWriter();
        ExpenseCsvWriter.writeTo(out, rows.stream());

        assertThat(out.toString()).isEqualTo(ExpenseCsvWriter.write(rows));
    }

    @Test
    void streamingPreservesStreamOrderAndTwoDecimalAmounts() {
        StringWriter out = new StringWriter();

        ExpenseCsvWriter.writeTo(
                out,
                Stream.of(
                        row("300", LocalDate.of(2026, 6, 3), Category.TRANSPORT),
                        row("20.50", LocalDate.of(2026, 6, 1), Category.RENT)));

        String[] lines = out.toString().split("\r\n");
        assertThat(lines).hasSize(3);
        assertThat(lines[0]).isEqualTo("id,date,category,amount");
        assertThat(lines[1]).endsWith("2026-06-03,TRANSPORT,300.00");
        assertThat(lines[2]).endsWith("2026-06-01,RENT,20.50");
    }
}
