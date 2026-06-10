package com.expensetracker.web.csv;

import com.expensetracker.web.dto.ExpenseResponse;
import java.math.RoundingMode;
import java.util.List;

/**
 * Renders a list of {@link ExpenseResponse} rows to CSV for the export endpoint
 * ({@code GET /api/expenses/export}, P4-1), matching the body in section 4 of
 * {@code docs/api-contracts.md}:
 *
 * <pre>
 * id,date,category,amount
 * 9f1c2e7a-...,2026-06-10,GROCERIES,1200.00
 * </pre>
 *
 * <p>Money stays exact decimal: {@code amount} is a {@link java.math.BigDecimal}
 * formatted to plain two-decimal text ({@code toPlainString} at scale 2), so no
 * float ever touches the value and no scientific notation can appear — the
 * money-precision rule from {@code CLAUDE.md} held all the way to the wire.
 *
 * <p>Fields are written with minimal RFC&nbsp;4180 quoting: a field is quoted only
 * when it contains a comma, quote, or newline (none of {@code UUID}/ISO-date/enum
 * ever do, but the helper is defensive). Rows are joined with {@code \r\n}, the
 * line terminator RFC&nbsp;4180 specifies for {@code text/csv}.
 */
public final class ExpenseCsvWriter {

    private static final String HEADER = "id,date,category,amount";
    private static final String LINE_SEPARATOR = "\r\n";

    private ExpenseCsvWriter() {}

    /** Renders the rows (already filtered/sorted) to a CSV document including the header line. */
    public static String write(List<ExpenseResponse> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append(LINE_SEPARATOR);
        for (ExpenseResponse row : rows) {
            sb.append(field(row.id().toString()))
                    .append(',')
                    .append(field(row.date().toString()))
                    .append(',')
                    .append(field(row.category().name()))
                    .append(',')
                    .append(field(amount(row)))
                    .append(LINE_SEPARATOR);
        }
        return sb.toString();
    }

    /** Two-decimal plain-string amount — exact, never scientific notation. */
    private static String amount(ExpenseResponse row) {
        return row.amount().setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    /** Quotes/escapes a field only when it contains a comma, quote, or newline (RFC 4180). */
    private static String field(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
