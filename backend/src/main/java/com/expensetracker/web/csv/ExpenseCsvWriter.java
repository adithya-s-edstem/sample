package com.expensetracker.web.csv;

import com.expensetracker.web.dto.ExpenseResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Stream;

/**
 * Renders {@link ExpenseResponse} rows to CSV for the export endpoint
 * ({@code GET /api/expenses/export}, P4-1/P4-2), matching the body in section 4 of
 * {@code docs/api-contracts.md}:
 *
 * <pre>
 * id,date,category,amount
 * 9f1c2e7a-...,2026-06-10,GROCERIES,1200.00
 * </pre>
 *
 * <p><b>Streaming (P4-2).</b> {@link #writeTo(Writer, Stream)} writes the header and
 * then each row straight to the supplied {@link Writer} as it is pulled from the
 * source {@link Stream}, never buffering the whole document — so a cursor-backed
 * stream from the repository flows to the HTTP output at constant memory regardless
 * of how many rows match. {@link #write(List)} is retained as a convenience that
 * renders a materialized list to a {@code String} (used where the full body is
 * small or needed in memory) and is defined in terms of the same row format.
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

    /**
     * Streams the header and each row to {@code out} as it is drawn from
     * {@code rows}, without buffering the document. The caller owns both the
     * {@code Writer} and the {@code Stream} (e.g. a cursor-backed repository stream
     * inside a read-only transaction) and is responsible for closing them.
     *
     * @throws UncheckedIOException if writing to {@code out} fails
     */
    public static void writeTo(Writer out, Stream<ExpenseResponse> rows) {
        try {
            out.write(HEADER);
            out.write(LINE_SEPARATOR);
            // forEachOrdered preserves the query's sort order while streaming.
            rows.forEachOrdered(row -> writeRow(out, row));
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed writing CSV export", e);
        }
    }

    /** Renders the rows (already filtered/sorted) to a CSV document including the header line. */
    public static String write(List<ExpenseResponse> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append(LINE_SEPARATOR);
        for (ExpenseResponse row : rows) {
            sb.append(row(row)).append(LINE_SEPARATOR);
        }
        return sb.toString();
    }

    private static void writeRow(Writer out, ExpenseResponse row) {
        try {
            out.write(row(row));
            out.write(LINE_SEPARATOR);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed writing CSV export", e);
        }
    }

    /** One CSV record (no line terminator): {@code id,date,category,amount}. */
    private static String row(ExpenseResponse row) {
        return field(row.id().toString())
                + ','
                + field(row.date().toString())
                + ','
                + field(row.category().name())
                + ','
                + field(amount(row));
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
