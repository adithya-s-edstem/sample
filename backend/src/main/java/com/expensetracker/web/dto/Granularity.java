package com.expensetracker.web.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Time-series bucket size for {@code GET /api/summary/trend}, matching the
 * {@code granularity} param in {@code docs/api-contracts.md}.
 *
 * <p>Serialized lowercase on the wire ({@code day} / {@code month}) per the
 * contract; the request binder accepts the same lowercase tokens
 * case-insensitively (see {@link Granularity#fromParam}).
 */
public enum Granularity {
    DAY,
    MONTH;

    /** Lowercase wire form ({@code day} / {@code month}) used in the response JSON. */
    @JsonValue
    public String wireValue() {
        return name().toLowerCase();
    }

    /**
     * Parses the request token (e.g. {@code day}, {@code month}) case-insensitively,
     * or {@code null} when the param is omitted. Throws
     * {@link IllegalArgumentException} for any other value so the global handler
     * maps it to a uniform 400.
     */
    public static Granularity fromParam(String value) {
        if (value == null) {
            return null;
        }
        return Granularity.valueOf(value.trim().toUpperCase());
    }
}
