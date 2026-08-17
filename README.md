# Order Platform – Full System

A modular Java order management system built as part of the ProgressSoft internship program.  
It demonstrates **layered architecture**, **multi‑module Maven**, **JDBC & JPA repositories**, **concurrent processing**, **file import**, **REST API with session & API‑key auth**, and a **thread‑safe rate limiter**.

---

## Module Structure

order-cli → order-service → order-persistence → order-domain
order-web → order-service (transitively pulls all others)
text


| Module | Contents |
|--------|----------|
| **order-domain** | `Order`, `Money` embeddable, all exceptions |
| **order-persistence** | Repository interfaces, in‑memory, JDBC, JPA implementations, DDL |
| **order-service** | Business logic, validation, file import, concurrency, service factory |
| **order-cli** | Command‑line interface (interactive menu) |
| **order-web** | REST API (`/api/orders`), auth filters, rate limiter, client page |

---

## Build & Run

### Prerequisites
- Java 11
- Maven 3.8+

### Build
```bash
git clone ...
cd phase-2
mvn clean install
```
### Run CLI
```bash

java -jar order-cli/target/order-cli-1.0-SNAPSHOT.jar
```
### Run Web (embedded Jetty)
```bash

mvn -pl order-web jetty:run
```
Then open http://localhost:8080/ to see the client page, or use curl.
Choose repository at runtime
```bash

mvn -pl order-web jetty:run -Drepo.type=jpa   # default is jdbc
```
### Testing

| Command | Scope | Description |
|---------|-------|-------------|
| `mvn test` | All tests | Runs all unit + integration tests |
| `mvn test -Dgroups="integration"` | Integration only | Runs only integration tests (H2, Jetty) |
| `mvn test -Dgroups="!integration"` | Unit only | Runs only unit tests (excludes integration) |

### Key Features:

1. CSV import – NIO‑based, malformed lines skipped with reasons.
2. JDBC repository – HikariCP pooling, PreparedStatement only, explicit transactions.
3. JPA repository – Hibernate, @Embeddable Money, explicit EntityManager transactions.
4. Concurrent batch processing – CompletableFuture, AtomicLong, ConcurrentHashMap.
5. REST API – GET list, GET by ID, POST place order (JSON).
6. Session + API‑key auth – AuthFilter with strategy pattern; POST requires auth, GET open.
7. Rate limiter – sliding‑window, thread‑safe, 429 Too Many Requests.
8. Custom cookie – lastViewedOrder set on GET by ID.
9. Proxy logging – java.lang.reflect.Proxy decorator for repository calls.

### Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Repository** | `Repository<ID,T>` | Abstracts data access, multiple implementations. |
| **Strategy** | `PaymentValidator` / `OrderEnricher` composition | Validators/enrichers combined at runtime. |
| **Decorator / Proxy** | `RepositoryProxy` | Cross‑cutting logging without changing repositories. |
| **Strategy (Auth)** | `AuthStrategy` | Session and API‑key authentication, easily extended. |
| **Factory** | `ServiceFactory` | Centralises service wiring. |

### Exception Hierarchy

- PaymentException (checked) – root for payment errors.
  - ValidationFailedException (checked) – input validation.
  - InsufficientFundsException (checked) – business rule.
  - GatewayTimeoutException (unchecked) – transient network failure.
- OrderNotFoundException (unchecked) – not found.
- ReconciliationRequiredException (checked) – charged but DB failed.
