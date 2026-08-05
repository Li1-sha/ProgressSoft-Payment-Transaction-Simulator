# Part 1 — Java Fundamentals Review

## 1. JDK vs JRE vs JVM

Think of it like layers:

- **JVM** = the engine that runs your code.
- **JRE** = the JVM + everything needed to *run* a Java program.
- **JDK** = the JRE + tools needed to *write and build* a Java program (like `javac`, the compiler).

Simple way to remember it: **JDK to build it, JRE to run it, JVM to actually execute it.**

## 2. Access Modifiers

Four levels, from most private to most open:

```java
public class Example {
    private String secret;     // only this class can see it
    String forFriends;         // only classes in the same folder/package
    protected int shared;      // same package + any subclass, even elsewhere
    public String open;        // anyone, anywhere
}
```

**Simple rule:** start with `private`. Only open it up (`protected` or `public`) if something outside the class actually needs it. In this project, every field in `Book` and `Library` is `private` — nothing is touched directly from outside; you always go through a method like `getTitle()` or `setBorrowed()`.

## 3. String vs StringBuilder

```java
// Slow way — makes a brand new String every single loop
String result = "";
for (int i = 0; i < 5000; i++) {
    result += i;
}

// Fast way — edits the same buffer instead of making new Strings
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 5000; i++) {
    sb.append(i);
}
String result2 = sb.toString();
```

**Why:** a `String` can't change once made — every `+=` throws the old one away and builds a whole new one. A `StringBuilder` just keeps adding onto the same thing. That's why `Library.catalogSummary()` uses `StringBuilder` instead of `+=` in its loop.

## 4. Object Lifecycle

Three simple stages:

1. **Created** — `new Book(...)` makes the object.
2. **Used** — as long as something still points to it, it's alive.
3. **Garbage collected** — once *nothing* points to it anymore, Java can clean it up.

```java
private static void demo() {
    Book temp = new Book("Refactoring", "Martin Fowler"); // created
    System.out.println(temp);                              // used
    // method ends here — temp disappears, nothing else points
    // to that Book, so it's now eligible for garbage collection
}
```

If that same `Book` had been added to `library`'s list instead, it would **stay alive** — the list is still pointing to it, even after `temp` goes out of scope.

## 5. `@Override`

Used above `toString()`, `equals()`, and `hashCode()` in `Book`.

It tells the compiler: *"I'm replacing a method that already exists in the parent class — double check I actually did that correctly."*

**Why it helps:** if you misspell it (like `eqauls` instead of `equals`), Java would normally just accept that as a brand new, unused method — no error, no warning. With `@Override`, Java catches the mistake immediately and refuses to compile, so you find out right away instead of the bug hiding silently.

## Encapsulation check

`Book` and `Library` have zero public fields. Everything is `private`, and all access goes through methods (getters, `setBorrowed()`, `addBook()`, `removeBook()`).