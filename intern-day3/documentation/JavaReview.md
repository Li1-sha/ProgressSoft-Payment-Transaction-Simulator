# Part 1 — Java Fundamentals Review

## 1. JDK vs JRE vs JVM

`JDK ⊃ JRE ⊃ JVM`

- **JVM** — executes compiled bytecode (`.class` files); platform-specific, "write once, run anywhere."
- **JRE** — JVM + standard library classes needed to *run* a program.
- **JDK** — JRE + dev tools (`javac`, `jar`, etc.) needed to *build* a program.

In this project: Maven needs a JDK to compile (`javac` runs under `mvn clean install`). Once built into a jar, anyone with just a JRE can run it (`java -jar target/intern-day3.jar`).

## 2. Access Modifiers

```java
package com.progressoft.demo;

public class AccessModifierDemo {
    private String internalId;        // this class only — default for fields
    String note = "same package";     // package-private — internal collaboration
    protected int retryCount = 0;     // package + subclasses — for designed-to-extend classes
    public AccessModifierDemo(String id) { this.internalId = id; }  // public — the real API
    public String getInternalId() { return internalId; }
}
```

```java
package com.progressoft.demo.sub;
import com.progressoft.demo.AccessModifierDemo;

public class AccessModifierSubclass extends AccessModifierDemo {
    public AccessModifierSubclass(String id) {
        super(id);
        this.retryCount++; // OK — protected is visible to subclasses in other packages
        // note and internalId would NOT be accessible here
    }
}
```

Rule of thumb: default to `private`; use package-private for tight in-package collaboration; `protected` only when designing for inheritance; `public` only for the intentional API. This project's `Book`/`Library` follow this — every field is `private`, accessed only via getters/setters.

## 3. String vs StringBuilder

```java
// BAD — O(n²): each += discards the old String and allocates a new one
String result = "";
for (int i = 0; i < 5000; i++) result += i + ",";

// GOOD — O(n): appends into one mutable buffer, one String built at the end
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 5000; i++) sb.append(i).append(",");
String result2 = sb.toString();
```

`String` is immutable — every concatenation creates a new object and copies everything so far. `StringBuilder` mutates an internal buffer in place. This project applies it directly: `Library.catalogSummary()` uses `StringBuilder` in its loop instead of `+=`.

## 4. Object Lifecycle

Creation → use → eligible for GC once no reachable reference remains.

```java
private static void demonstrateObjectLifecycle() {
    Book temp = new Book("Refactoring", "Martin Fowler"); // created
    System.out.println("Temporary book created: " + temp);  // used
    // temp is local to this method; once it returns, the reference is gone
    // and the Book becomes eligible for garbage collection.
}
```

Contrast: a `Book` passed to `library.addBook(book)` stays alive after the local variable goes out of scope, because the `Library`'s list still references it — not GC-eligible until removed or the `Library` itself is unreachable.

## 5. Annotation: `@Override`

Used on `Book.toString()`, `equals()`, `hashCode()`. At compile time it makes the compiler verify the method actually overrides something in the superclass (`Object`). Without it, a typo like `eqauls(Object o)` would silently compile as a new, unrelated method instead of raising an error — quietly breaking the case-insensitive equality this project relies on.

## Encapsulation check

No public fields in `Book` or `Library` — every field is `private`; all access goes through getters or controlled setters (`setBorrowed()`, `addBook()`, `removeBook()`).