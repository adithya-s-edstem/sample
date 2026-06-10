# Expense Tracker — API Contracts

_Last updated: 2026-06-10_

## Conventions

- **Base path**: `/api`
- **Format**: JSON request/response (UTF-8). CSV export is the one exception.
- **IDs**: `UUID` strings.
- **Money**: `amount` is a decimal number in **INR**, two decimal places
  (server uses `BigDecimal` / `NUMERIC(12,2)`). Must be **> 0**.
- **Dates**: `date` is an ISO calendar date `YYYY-MM-DD`. Timestamps
  (`createdAt`, `updatedAt`) are ISO-8601 instants (UTC).
- **Categories** (enum, fixed set):
  `FOOD`, `TRANSPORT`, `RENT`, `UTILITIES`, `GROCERIES`, `ENTERTAINMENT`,
  `HEALTH`, `SHOPPING`, `OTHER`.
- **Auth**: none in v1 (single-user).
- **CORS**: Vite dev origin allowed in development.

## Data Transfer Objects

### Expense (response)
```json
{
  "id": "9f1c2e7a-3b6d-4f2a-8c11-2a7e9d0b4c55",
  "amount": 1200.00,
  "date": "2026-06-10",
  "category": "GROCERIES",
  "createdAt": "2026-06-10T09:15:30Z",
  "updatedAt": "2026-06-10T09:15:30Z"
}
```

### ExpenseRequest (create / update body)
```json
{
  "amount": 1200.00,
  "date": "2026-06-10",
  "category": "GROCERIES"
}
```

Validation:
- `amount` — required, decimal, **> 0**.
- `date` — required, valid `YYYY-MM-DD`.
- `category` — required, must be one of the enum values.

### Error response (uniform)
```json
{
  "timestamp": "2026-06-10T09:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "amount must be greater than 0",
  "path": "/api/expenses",
  "fieldErrors": [
    { "field": "amount", "message": "must be greater than 0" }
  ]
}
```

---

## 1. Expenses — CRUD

### `POST /api/expenses` — create
- **Body**: `ExpenseRequest`
- **201 Created** → `Expense`
- **400** validation error.

### `GET /api/expenses/{id}` — get one
- **200** → `Expense`
- **404** if not found.

### `PUT /api/expenses/{id}` — update (full)
- **Body**: `ExpenseRequest`
- **200** → updated `Expense`
- **400** validation, **404** not found.

### `DELETE /api/expenses/{id}` — delete
- **204 No Content**
- **404** if not found.

---

## 2. Expenses — List (filter + paginate)

### `GET /api/expenses`
Query parameters (all optional unless noted):

| Param        | Type     | Description                                          |
|--------------|----------|------------------------------------------------------|
| `from`       | date     | Start of date range (inclusive). Defaults to current month start if omitted. |
| `to`         | date     | End of date range (inclusive). Defaults to current month end. |
| `category`   | enum     | Filter by a single category.                         |
| `minAmount`  | decimal  | Minimum amount (inclusive).                          |
| `maxAmount`  | decimal  | Maximum amount (inclusive).                          |
| `q`          | string   | Free-text search (reserved; matches category for now). |
| `sort`       | string   | e.g. `date,desc` (default), `amount,asc`.            |
| `page`       | int      | 0-based page index (default `0`).                    |
| `size`       | int      | Page size (default `50`, max `200`).                 |

**200** → paginated response:
```json
{
  "content": [ /* Expense[] */ ],
  "page": 0,
  "size": 50,
  "totalElements": 137,
  "totalPages": 3,
  "sort": "date,desc"
}
```

---

## 3. Summary / Reports (dedicated aggregation endpoints)

All summary endpoints accept the same date-range params (`from`, `to`); they
default to the **current month** when omitted. The backend computes these so the
frontend receives ready-to-chart JSON.

### `GET /api/summary` — headline numbers for a period
**200**:
```json
{
  "from": "2026-06-01",
  "to": "2026-06-30",
  "total": 24500.00,
  "count": 42,
  "currency": "INR"
}
```

### `GET /api/summary/by-category` — category breakdown (donut)
Optional same range params.
**200**:
```json
{
  "from": "2026-06-01",
  "to": "2026-06-30",
  "total": 24500.00,
  "categories": [
    { "category": "RENT",      "total": 12000.00, "count": 1,  "percent": 48.98 },
    { "category": "GROCERIES", "total":  5200.00, "count": 8,  "percent": 21.22 },
    { "category": "FOOD",      "total":  3300.00, "count": 12, "percent": 13.47 }
  ]
}
```

### `GET /api/summary/trend` — spending over time (bar/line)
Extra param: `granularity` = `day` | `month` (default `day` within a month
range, `month` across longer ranges).
**200**:
```json
{
  "from": "2026-01-01",
  "to": "2026-06-30",
  "granularity": "month",
  "points": [
    { "period": "2026-01", "total": 21000.00 },
    { "period": "2026-02", "total": 18750.00 },
    { "period": "2026-03", "total": 23100.00 }
  ]
}
```

---

## 4. Export

### `GET /api/expenses/export` — CSV download
- Accepts the **same filter params** as the list endpoint (`from`, `to`,
  `category`, `minAmount`, `maxAmount`, `q`). No pagination — exports all matches.
- **200** with:
  - `Content-Type: text/csv`
  - `Content-Disposition: attachment; filename="expenses-YYYY-MM-DD.csv"`
- **Body** (example):
```csv
id,date,category,amount
9f1c2e7a-...,2026-06-10,GROCERIES,1200.00
1a2b3c4d-...,2026-06-09,TRANSPORT,300.00
```

---

## 5. Reference

### `GET /api/categories` — list predefined categories
Convenience for populating the dropdown.
**200**:
```json
["FOOD","TRANSPORT","RENT","UTILITIES","GROCERIES","ENTERTAINMENT","HEALTH","SHOPPING","OTHER"]
```

---

## Status Code Summary

| Code | Meaning                                   |
|------|-------------------------------------------|
| 200  | OK (reads, updates, export)               |
| 201  | Created (new expense)                     |
| 204  | No Content (delete)                       |
| 400  | Validation / bad request                  |
| 404  | Resource not found                        |
| 500  | Unexpected server error                   |

## Notes / Deferred

- `q` free-text search is reserved; v1 effectively filters by category/range.
  (Expanded once description/notes fields are added — currently out of scope.)
- No auth in v1; if multi-user is added later, endpoints become user-scoped.
- OpenAPI/Swagger UI (springdoc) can expose this contract interactively.
