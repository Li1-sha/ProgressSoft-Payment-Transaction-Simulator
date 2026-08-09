# Mock vs Spy

## Mock (Mockito)

- **What it is**: a complete fake object.
- **How to create**: `@Mock` annotation or `Mockito.mock(Class)`.
- **Behaviour**:
    - No real code runs.
    - Every method returns default: `false`, `0`, `null`.
    - You must **stub** what you want (`when(...).thenReturn(...)`).
- **When to use**:
    - Testing a class that depends on external systems (DB, API, payment, email).
    - You want **isolation** – only test the logic, not the dependency.
    - You need **predictable** responses (success, failure, exceptions).
- **Pros**: fast, no side‑effects, full control.
- **Cons**: you don't test the real integration (that's for integration tests).

---

## Spy (Mockito / manual)

- **What it is**: a wrapper around a **real** object.
- **How to create (Mockito)**: `@Spy` annotation or `spy(realObject)`.
- **Default behaviour**: real methods execute (side‑effects happen).
- **You can still verify** calls (like with mocks).
- **My experience**: Mockito spy failed on Java 25 (bytecode issue).  
  → I implemented a **manual spy** using a lambda:

```java
boolean[] called = { false };
PaymentGateway spy = amount -> {
    called[0] = true;
    return realGateway.charge(amount);
};