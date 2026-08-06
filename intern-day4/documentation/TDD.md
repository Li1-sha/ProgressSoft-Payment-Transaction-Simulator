# TDD Overview

## The idea
Write the test **before** the code. Let the test tell you what to build.

## The cycle: Red → Green → Refactor

| Step | Do this | Result |
|------|---------|--------|
| Red | Write a test for code that doesn't exist yet | Test fails |
| Green | Write the smallest code to pass it | Test passes |
| Refactor | Clean up the code | Test still passes |

Repeat for the next small piece of behavior.

## Example: adding `removeBook()`

**1. Red — write the test first**
```java
@Test
public void removingExistingTitleReturnsTrue() {
    library.addBook(new Book("The Hobbit", "Tolkien"));
    assertTrue(library.removeBook("The Hobbit"));
}
```
Doesn't even compile yet — `removeBook()` doesn't exist. That's fine, that's "red."

**2. Green — write just enough to pass**
```java
public boolean removeBook(String title) {
    return books.removeIf(b -> b.getTitle().equalsIgnoreCase(title));
}
```
Run the test — passes. "Green."

**3. Refactor**
Already clean, nothing to change. If it were messy, you'd clean it up now and re-run the test to confirm it's still green.

## Why do it this way

- No leftover code — everything exists because a test needed it
- Instant safety net — a broken change fails a test right away
- Pushes you toward small, simple methods (hard-to-test code is a warning sign)

## One-line rule

**Red → Green → Refactor → Repeat.**