# Order Service Module – System Overview

## What the System Does
This module is a **production-ready order processing engine** built in pure Java. Its job is to:

1. Accept an `Order` (customer name, amount, currency).
2. **Enrich** it with defaults (e.g., set currency to OMR if missing).
3. **Validate** it against configurable business rules (e.g., positive amount, max limit, allowed currency).
4. **Charge** a payment gateway (simulated for this foundation).
5. **Persist** the successful order in a thread-safe in‑memory store.

The entire system is built without shortcuts—it uses **generics**, **functional composition**, and a **checked exception hierarchy** to ensure the code is swapable for JDBC later, easy to extend, and forces callers to handle payment failures explicitly.

---

## How It Works – The Core Flow

The heart of the system is `OrderService.placeOrder()`. When an order arrives:

1. **Enrichment (`OrderEnricher`)**
    - Applies preprocessing, like setting a default currency (`"OMR"`) if none is provided.

2. **Validation (`PaymentValidator`)**
    - Runs a **single composed validator** built from multiple rules.
    - If any rule fails, it throws a `ValidationFailedException` **immediately** (short‑circuit).

3. **Payment Processing (`PaymentGateway`)**
    - Attempts to charge the order.
    - Throws `InsufficientFundsException` or `GatewayTimeoutException` on failure.

4. **Persistence (`Repository`)**
    - Saves the order to an in‑memory `ConcurrentHashMap` with an auto‑generated ID.

All exceptions are **checked**, so the calling code (the `Main` menu or a controller) **must** explicitly handle or re‑declare them.

---

## Component Breakdown

### Part A – Persistence Foundation
- **`Identifiable<ID>`** – A contract that forces every entity to expose `getId()` and `setId(ID)`.
- **`Repository<T extends Identifiable<ID>, ID>`** – A generic CRUD interface with self‑bound generics (no raw types).
    - Includes a wildcard method: `void deleteAll(Collection<? extends ID> ids)` – uses covariance (`? extends`) because we only *read* from the collection, maximising caller flexibility.
- **`InMemoryRepository`** – Thread‑safe implementation using `ConcurrentHashMap`. It accepts a `Supplier<ID>` for flexible ID generation (e.g., `AtomicLong` or `UUID`).
- **`Order`** – The domain entity implementing `Identifiable<Long>`.

**Key Design**: The service depends on the `OrderRepository` **interface**. Swapping to a `JdbcOrderRepository` later requires **zero changes** to `OrderService` or callers.

---

### Part B – Functional Validation
- **`PaymentValidator`** (Custom FI #1) – Validates an `Order` and throws `ValidationFailedException`.
    - Provides a default `.and(PaymentValidator other)` method to chain rules, mirroring `Predicate.and()`.
- **`OrderEnricher`** (Custom FI #2) – Transforms/preprocesses an `Order`.
    - Provides a default `.andThen(OrderEnricher after)` method to chain enrichment steps.
- **`Validators`** – A utility factory holding 3 concrete rules:  
  `positiveAmount()` | `maxLimit(double)` | `currencyCheck(String...)`.

**Key Design**: Instead of a giant `if`‑chain inside `OrderService`, we compose rules via `.and()`:
```java
PaymentValidator composed = Validators.positiveAmount()
    .and(Validators.maxLimit(10000.0))
    .and(Validators.currencyCheck("OMR", "EUR", "USD"));
```
The service calls **one** `validator.validate(order)` – it never knows how many rules are inside.

---

### Part C – Exception Taxonomy
- **Base**: `PaymentException` (abstract, checked) – holds an `errorCode`.
- **Subclasses** (all checked):
    - `ValidationFailedException` – carries `fieldName`, `rejectedValue`, `reason`.
    - `InsufficientFundsException` – carries `requiredAmount`, `availableAmount`.
    - `GatewayTimeoutException` – carries `endpoint`, `timeoutMillis`, `operation`.

**Key Design**: All exceptions are **checked** (`extends Exception`). In financial systems, failures like insufficient funds or gateway timeouts **must** be explicitly handled by the caller (retry, fallback, user notification). Checked exceptions enforce this discipline and prevent accidental silent failures.

---

## Constraints (How We Built It)
-  **No raw types** – fully parameterized generics everywhere.
-  **No unchecked‑cast suppressions** – zero `@SuppressWarnings("unchecked")`.
-  **No `Object`‑typed collections** – all maps and lists are strongly typed.
-  **Swappable for JDBC** – the service uses the interface, not the concrete store.
-  **Test‑first** – JUnit 5 tests exist for all logic (validators, exceptions, propagation).
-  **Zero empty/bare catch blocks** – no `catch (Exception e)` or empty `catch` anywhere.

---

## Running the System
```bash
# Compile
mvn clean compile

# Run tests (should be green)
mvn test

# Run the interactive console demo
mvn exec:java -Dexec.mainClass="com.progressoft.Main"
```

In the interactive demo, you can:
- Place orders with different amounts/currencies.
- See the validators reject negative amounts, high amounts, or disallowed currencies.
- Witness the exception hierarchy in action when the dummy payment gateway fails.

---

## Summary Architecture Layer Diagram
```
[Application]   (Wires everything together)
       │
       ▼
[OrderService]  (Orchestrates: Enrich → Validate → Charge → Save)
       │
       ├──► [OrderEnricher]   (Functional composition)
       ├──► [PaymentValidator] (Functional composition / .and() chaining)
       ├──► [PaymentGateway]   (Throws checked exceptions)
       └──► [OrderRepository]  (Interface – swappable)
                │
                ▼
        [InMemoryRepository]   (ConcurrentHashMap – JDBC later)
```

---

This system provides a **solid, type‑safe, and extensible foundation** designed to handle real‑world business complexity without sacrificing cleanliness or testability.