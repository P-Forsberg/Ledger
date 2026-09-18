# Ledger

A small account-to-account transfer service, built to explore how money movement is modelled correctly rather than conveniently.

**Status:** work in progress. See [Roadmap](#roadmap) for what is built and what is not.

## Why this exists

Most tutorial banking projects model a transfer as a row you can create and delete, and a balance as a number you overwrite. Both are wrong in ways that matter, and this project is an attempt to get them right:

- A ledger is append-only. A transfer that happened cannot un-happen.
- A balance is not a field. It is the consequence of everything that has been recorded.
- Money moves under concurrency, and two requests that read the same balance at the same time must not both succeed.

## Domain model

Four tables, described in full in `src/main/resources/db/migration/V1__initial_schema.sql`.

| Table | Purpose |
|---|---|
| `users` | Authentication identity |
| `accounts` | An owned balance in a single currency |
| `transfers` | The intent: move X from account A to account B |
| `entries` | The record: what each account actually gained or lost |

A transfer never touches a balance column, because there isn't one. It writes exactly two rows in `entries`: one negative on the source account, one positive on the target. Both are written in the same database transaction, so the pair either exists or does not.

## Design decisions

### Double-entry: every transfer sums to zero

Each transfer produces two entries whose amounts add up to zero. Money is therefore conserved by construction: it can move between accounts, but it cannot enter or leave the system by accident.

This gives one system-wide invariant that can be asserted in a test:

```
SELECT SUM(amount) FROM entries;  -- must always be 0
```

A bug that loses or invents money breaks that sum. A test asserting it catches an entire class of errors that per-endpoint tests would not.

### Reversal instead of deletion

There is no `DELETE /transfers/{id}`, and there never will be. Deleting a transfer would remove the evidence that money moved, which is the opposite of what a ledger is for.

To undo a transfer, `POST /transfers/{id}/reversal` creates a *new* transfer in the opposite direction, linked to the original via `reverses_transfer_id`. Both rows stay in history, and the account's balance returns to where it was. A database constraint enforces that a given transfer can be reversed at most once.

### Idempotency

Clients send an `Idempotency-Key` header with each transfer. The key is stored with the transfer and is unique per user.

If the same key arrives again — a retried request, a double-tapped button, a client that timed out and gave up before the response arrived — the original transfer is returned unchanged and no new money moves. This follows the same pattern Stripe's API uses, for the same reason: over an unreliable network, "did my request go through?" is unanswerable from the client side, so retrying must be safe.

### Concurrency

Two transfers leaving the same account at the same time can both read a sufficient balance and both succeed, leaving the account overdrawn. This is the classic lost-update problem and it does not show up in single-threaded tests.

The source account row is locked with `SELECT ... FOR UPDATE` before the balance is checked, so the second transaction waits for the first to commit and then sees the real balance. `accounts.version` is present for optimistic locking where pessimistic locking is too coarse.

The test suite runs concurrent transfers against the same account and asserts the resulting balance, because this is not something that can be verified by reading the code.

### Money is stored as integers

Amounts are `bigint`, in the currency's minor unit — öre for SEK, cents for EUR. Floating point is never used for money: `0.1 + 0.2` is not `0.3` in binary floating point, and rounding error in a ledger is indistinguishable from theft.

### Balances are derived

An account balance is `SUM(amount)` over its entries. This is correct by definition and cannot drift from the transaction history.

The honest trade-off: this does not scale. An account with millions of entries would need a running balance maintained alongside, or periodic balance snapshots to sum forward from. At this project's size, correctness is worth more than the optimisation, and the schema leaves room to add it later.

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Create a user |
| `POST` | `/api/auth/login` | Exchange credentials for a JWT |
| `GET` | `/api/accounts/me` | The caller's accounts and balances |
| `POST` | `/api/transfers` | Create a transfer (requires `Idempotency-Key`) |
| `GET` | `/api/transfers` | The caller's transfers, paginated |
| `GET` | `/api/transfers/{id}` | A single transfer |
| `POST` | `/api/transfers/{id}/reversal` | Reverse a transfer |

Every read is scoped to the authenticated user's own accounts. Requesting a transfer belonging to someone else returns `403`, and there is a test that proves it — looking up a resource by id without checking ownership is the most common authorisation hole in small services.

Interactive documentation is available at `/swagger-ui.html` when the application is running.

## Tech stack

Java 21, Spring Boot 4.1, Spring Security with JWT, Spring Data JPA, PostgreSQL, Flyway, Maven.

Tests run against a real PostgreSQL instance via Testcontainers rather than an in-memory database, because the locking behaviour this project depends on is not reproduced by H2. A green test suite on the wrong database would be worse than no tests at all.

## Running locally

Requires JDK 21 and Docker.

```bash
./mvnw clean verify     # runs the full test suite, including Testcontainers
./mvnw spring-boot:run  # starts the application
```

Flyway applies the schema on startup.

## Roadmap

- [x] Project scaffold, Testcontainers wiring
- [ ] Schema and entities
- [ ] Registration, login, JWT
- [ ] Transfers with idempotency
- [ ] Pessimistic locking and concurrency tests
- [ ] Reversals
- [ ] OpenAPI documentation
- [ ] GitHub Actions pipeline

---

Built by [Pontus Forsberg](https://github.com/P-Forsberg).
