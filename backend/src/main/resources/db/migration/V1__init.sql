-- Initial schema for the Expense Tracker.
--
-- Single table, single-user, no auth (v1). Money is exact decimal: amount is
-- NUMERIC(12,2) with a CHECK enforcing the API contract's `amount > 0` rule at
-- the database level. Timestamps are TIMESTAMPTZ (stored/read in UTC) to match
-- the ISO-8601-UTC API convention and the JPA `Instant` mapping.
--
-- Column names are snake_case; the JPA entity (P1-2) maps to this schema with
-- Hibernate ddl-auto=validate, so types here must match the entity exactly.

CREATE TABLE expenses (
    id         UUID           NOT NULL DEFAULT gen_random_uuid(),
    amount     NUMERIC(12, 2) NOT NULL,
    date       DATE           NOT NULL,
    category   VARCHAR(32)    NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL,
    updated_at TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_expenses PRIMARY KEY (id),
    CONSTRAINT ck_expenses_amount_positive CHECK (amount > 0)
);

-- Date-range filtering and monthly grouping are the dominant read pattern.
CREATE INDEX idx_expenses_date ON expenses (date);

-- Category filtering and per-category breakdowns (the donut summary).
CREATE INDEX idx_expenses_category ON expenses (category);
