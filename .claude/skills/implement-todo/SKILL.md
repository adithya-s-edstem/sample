---
name: implement-todo
description: End-to-end implementation of a single assigned task from todo.md — gathers context (conventions, current coverage, requirements) via subagents, syncs main, branches, implements with tests per testing-plan.md, makes atomic commits, pushes, and opens a PR. Use when the user says "implement P2-3", "work on todo P1-2", "pick up <todo id>", "do my assigned todo item", or hands you a task ID from todo.md to build.
---

# Implement a todo.md task

Take ONE assigned task from `todo.md` (e.g. `P2-3`, `P1-2`, `X-1`) from spec → branch →
tested implementation → atomic commits → pushed branch → open PR. The repo docs are the
source of truth; match them exactly.

## Inputs

The task ID is passed as an argument (e.g. `/implement-todo P2-3`). If none is given, read
`todo.md`, list the unchecked tasks, and ask the user which one to implement. Do not guess.

## Step 1 — Read the assigned item

Read `todo.md` and locate the task by its ID. Capture:

- The exact task text and which **Phase** it belongs to.
- Any sibling tasks in the same phase that are prerequisites (lower-numbered, unchecked).
- The **Exit** criterion for the phase — your implementation should move toward it.

If the task depends on unchecked prerequisite tasks (e.g. `P2-3` needs `P2-1` DTOs), surface
that to the user before proceeding — implementing out of order may not be possible.

## Step 2 — Gather context with subagents (in parallel)

Launch three `Explore` subagents **concurrently** (one message, multiple Agent calls). Each
returns a written summary, not file dumps:

1. **Coding conventions** — "Read `CLAUDE.md`, `technical-architecture.md`, and `api-contracts.md`.
   Summarize the binding conventions relevant to implementing `<task text>`: money handling
   (`BigDecimal`/`NUMERIC(12,2)`, INR, `> 0`), API base path `/api`, UUID string IDs, date/
   timestamp formats, the fixed `Category` enum, the uniform error shape, current-month
   defaulting, and the stack (Spring Boot 3.x / Java 21 / React+TS / Tailwind / Recharts /
   TanStack Query). Also report any existing code style patterns you find in `/backend` or
   `/frontend` if they exist yet."

2. **Current coverage** — "Determine what already exists for `<task text>`. Search `/backend`
   and `/frontend` (and the whole repo) for any code, entities, endpoints, components, or tests
   that already implement or partially implement this task. Report exactly what exists, what's
   missing, and the files I'll need to touch or create. If no project scaffolding exists yet,
   say so."

3. **Requirements** — "Read `requirements.md`, `solution.md`, and the relevant parts of
   `api-contracts.md` / `testing-plan.md`. Summarize the product requirements, acceptance
   behavior, and test expectations that apply specifically to `<task text>`."

Wait for all three. Reconcile their findings into a short implementation outline before writing code.

## Step 3 — Sync main

> Note: `CLAUDE.md` claims there's no app code yet; the repo may still be docs-only. The git
> steps below are still correct — branch off `main` regardless.

Run:

```
git fetch origin
git checkout main
git pull --ff-only origin main
git status   # confirm clean
```

If the working tree is dirty, stop and ask the user how to proceed (stash vs. commit) — never
discard their changes.

## Step 4 — Create the work branch

Branch name format: `todo-{id}` lowercased (e.g. task `P2-3` → `todo-p2-3`).

```
git checkout -b todo-p2-3
```

If the branch already exists locally or on origin, ask the user whether to reuse, rename, or abort.

## Step 5 — Implement

Implement the task to satisfy its requirements and move the phase toward its Exit criterion,
strictly honoring the conventions from Step 2:

- Money is exact decimal end-to-end — `BigDecimal` / `NUMERIC(12,2)`, never floats.
- API base path `/api`; JSON (except CSV export); UUID string IDs; dates `YYYY-MM-DD`;
  timestamps ISO-8601 UTC.
- Categories are the fixed enum; errors use the uniform shape; list/summary/export default to
  the current month.
- Match `api-contracts.md` **exactly** for any endpoint, DTO, status code, or error body.

Follow the backend-first build order. Keep changes scoped to this one task — don't pull in
unrelated work.

## Step 6 — Write tests per testing-plan.md

Consult `testing-plan.md` and write the tests appropriate to the task's layer:

- **Backend unit** (JUnit 5 + Mockito): service logic, aggregation, CSV, mappers.
- **Backend integration** (Spring Boot Test, `@WebMvcTest`, Testcontainers Postgres): repository
  queries, migrations, aggregation endpoints.
- **Contract** (MockMvc vs. `api-contracts.md`): status codes, body shapes, validation/error JSON.
- **Frontend** (Vitest + Testing Library, MSW): components, hooks, formatting utils.
- **E2E** (Playwright): only if the task is an explicit E2E flow.
- **Money/decimal precision**: whenever the task touches money, add/extend the precision suite
  (`0.01` accepted, `0`/negative rejected, exact round-trip, 2-decimal CSV).

Run the relevant test command and confirm green before committing:

- Backend: `./mvnw verify` (or `./mvnw test` for a faster inner loop).
- Frontend: `npm run test` / `vitest run`.

Report real results — if tests fail, fix them or say so; never claim green when they aren't.

## Step 7 — Atomic commits

Commit in small, independently revertible units (each commit builds and is self-contained), e.g.:

1. schema/entity/scaffolding
2. core logic / endpoint / component
3. tests
4. docs/wiring

Use clear messages referencing the task ID, e.g. `P2-3: add ExpenseController CRUD endpoints`.
Stage deliberately (`git add <paths>`) — don't blanket-commit unrelated files. Do not amend;
prefer new commits. Don't add co-author/attribution lines unless the user asks.

## Step 8 — Push

```
git push -u origin todo-p2-3
```

## Step 9 — Update CLAUDE.md if this task changed something it documents

Before raising the PR, check whether this task changed anything `CLAUDE.md` describes — and
update it **only if relevant**. CLAUDE.md is the entry point future instances read first, so it
must stay accurate, but don't churn it for changes it doesn't cover.

Update it when the task, for example:

- moves the build progress forward (a phase/area is now built that CLAUDE.md still calls
  unbuilt, or "not yet built" no longer holds);
- adds or changes a **command** (build/run/test/format, a new script or Maven goal);
- introduces a new **package/layer, module, or significant file** that changes the documented
  structure;
- changes a **convention or architectural decision** already captured there (and, per the
  guardrails, the underlying doc too).

Do **not** touch CLAUDE.md for routine within-the-lines work (a new test, an internal method,
a bugfix) that none of its sections describe.

If you edit it, keep the change tight and factual, then commit and push it onto the work branch
so it lands in this PR:

```
git add CLAUDE.md
git commit -m "<task id>: update CLAUDE.md for <what changed>"
git push
```

If nothing relevant changed, say so explicitly and move on — no edit, no empty commit.

## Step 10 — Open a PR

Use `gh pr create`. Base = `main`, head = the work branch. Title: `<task id>: <short summary>`.
Body should include:

- **Task**: the `todo.md` ID and text.
- **What changed**: bulleted summary of the implementation.
- **Tests**: which layers were added/run and their result.
- **Conventions**: note adherence to money/API/error conventions where relevant.
- **Closes/relates**: link the phase Exit criterion if it's now met.

Example:

```
gh pr create --base main --head todo-p2-3 --title "P2-3: ExpenseController CRUD endpoints" --body "..."
```

Return the PR URL to the user.

## Step 11 — Offer to check off the task

Ask whether to tick the task's checkbox in `todo.md` (`- [ ]` → `- [x]`) and note the Owner.
Only do this if the user confirms; commit it as part of the PR branch if so.

## Guardrails

- One task per run. If the user names several, do them sequentially as separate branches/PRs.
- If a prerequisite is missing or the repo isn't scaffolded yet for this task, say so and ask
  before proceeding — don't fabricate scaffolding the phase ordering doesn't call for.
- If `git`, `gh`, or the remote isn't available/authenticated, stop at the failing step and tell
  the user what to run (e.g. suggest `! gh auth login`).
- Keep the docs authoritative: if implementing forces a decision that diverges from a doc, flag
  it and update the doc in the same PR.
