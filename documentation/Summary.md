# Phase 1 & 2 – Learning Summary

---

## Core Java Foundations

- Designed a generic `Repository<ID, T extends Identifiable<ID>>` with self‑bound generics – learned how to write type‑safe, reusable abstractions.
- Built a 3‑level exception hierarchy where each subclass carries typed data beyond a simple message – understood checked vs unchecked:
    - Validation failures and business rule violations → checked (caller must handle).
    - Transient network failures → unchecked (let retry wrappers handle).
- Implemented `equals`, `hashCode`, `toString` on `Order` with careful null‑ID handling – equality must work both before and after persistence.

---

## Functional Programming with Streams

- Processed batches using pure streams – `filter`, `groupingBy`, `partitioningBy`, `summingDouble` – without writing a single loop.
- Rejected orders carried their reasons through the pipeline – integrated error handling into functional flows.
- Learned the difference between `summingDouble` (specialised, efficient) and `reducing` (general, verbose).

---

## Concurrency and Thread Safety

- Processed batches concurrently with `ExecutorService` and `CompletableFuture`.
- Used `CompletableFuture.allOf()` to wait for all tasks – partial failures captured as typed results, so `allOf` never fails.
- Managed shared state with `AtomicLong` and `ConcurrentHashMap<String, DoubleAdder>` – avoided blanket `synchronized`.
- Repeated race‑condition test 20× proved thread safety.

---

## File I/O with NIO

- Read CSV files using `Files.newBufferedReader()` (NIO channel) – never used legacy `FileReader`.
- Handled malformed lines gracefully – skipped with reasons, no crashes.
- Parsed orders fed directly into the stream pipeline – Day 2 and Day 3 compose cleanly.

---

## JDBC, Transactions, and Connection Pooling

- Implemented `JdbcOrderRepository` with HikariCP connection pooling – no `DriverManager.getConnection()` per call.
- All SQL via `PreparedStatement` with bound parameters – zero SQL injection risk.
- Explicit transaction control: `setAutoCommit(false)`, `commit()`, `rollback()` in `placeOrder`.
- After a successful external payment, DB failure cannot be undone – flagged with `ReconciliationRequiredException`.
- Forced‑failure rollback test proved no orphaned rows remain.

---

## Multi‑module Maven Architecture

- Split project into four modules: `order-domain`, `order-persistence`, `order-service`, `order-cli`.
- Parent POM manages versions; each child declares only what it uses.
- Dependency graph acyclic: `order-cli → order-service → order-persistence → order-domain`.
- Documented module graph in README – the actual deliverable.

---

## Cross‑cutting Concerns with Dynamic Proxy

- Used `java.lang.reflect.Proxy` to create `RepositoryProxy` decorator.
- Logs every repository method call – method name, arguments, duration, outcome.
- Applied without touching `InMemoryOrderRepository` or `JdbcOrderRepository`.
- Learned that cross‑cutting concerns can be woven at runtime – the honest use case for Decorator.

---

## Testing and Test Coverage

- Unit tests, integration tests (`@Tag("integration")`), repeated race‑condition tests.
- Mockito mocks isolated `OrderService` from `PaymentGateway`; spies verified real gateway execution.
- Rollback test forced DB failure after successful charge – asserted no rows persisted.
- Overall coverage **87.4%** (above 85% bar).

---

## Git and Collaboration

- Branch creation, manual conflict resolution, commit messages with convention.
- No build artifacts (`target/`, `.idea/`) committed – `.gitignore` in place.
- History shows red‑green‑refactor commits – test‑driven approach.

---

## Design Patterns and Clean Code

- **Strategy** – `PaymentValidator` and `OrderEnricher` via `and()` / `andThen()` composition.
- **Decorator / Proxy** – `RepositoryProxy` for cross‑cutting logging.
- **Repository** – generic interface abstracts data access.
- **Exception hierarchy** – structured typed exceptions.
- No public fields – all encapsulated with getters/setters.
- No god classes – responsibilities separated across modules.
- Access modifiers used consistently (no unnecessary `public`).

---

## Key Lessons Learned

- Design interfaces early – `TransactionalOrderRepository` should have been introduced before the proxy.
- Manual conflict resolution teaches more than auto‑merge.
- Explicit transaction control forces you to think about distributed consistency.
- Dynamic proxies are powerful but require careful interface design.
- Integration tests with real databases catch problems unit tests miss.

---

## Final Build Status

- `mvn clean verify` – **33 tests run, 0 failures, 0 errors**.
- All modules compile and build correctly.
