package com.progressoft;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Main {
    private static final Library library = new Library();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        runRequiredDemo();
        runMenu();
        scanner.close();
    }

    /** Satisfies the spec's required demo: 3 books, catalog, borrow one, count, lifecycle. */
    private static void runRequiredDemo() {
        library.addBook(new Book("The Great Gatsby", "F. Scott Fitzgerald"));
        library.addBook(new Book("The Hobbit", "J.R.R. Tolkien"));
        library.addBook(new Book("Pride and Prejudice", "Jane Austen"));

        System.out.println(library.catalogSummary());

        library.searchByTitle("The Hobbit").ifPresent(book -> book.setBorrowed(true));
        System.out.println("\nBorrowed count: " + library.borrowedCount());

        demonstrateObjectLifecycle();
        System.out.println();
    }

    private static void demonstrateObjectLifecycle() {
        Book temp = new Book("Refactoring", "Martin Fowler");
        System.out.println("Temporary book created: " + temp);
        // temp is a local variable scoped to this method only — nothing else
        // stores a reference to it, so once this method returns, the reference
        // disappears and the Book becomes eligible for garbage collection.
    }

    private static void runMenu() {
        System.out.println("=== Library Manager ===");
        boolean running = true;
        while (running) {
            printMenu();
            switch (readInt("Choice: ")) {
                case 1 -> addBook();
                case 2 -> removeBook();
                case 3 -> listBooks();
                case 4 -> searchBook();
                case 5 -> System.out.println("Borrowed count: " + library.borrowedCount());
                case 6 -> setBorrowed(true);
                case 7 -> setBorrowed(false);
                case 8 -> System.out.println(library.catalogSummary());
                case 0 -> { System.out.println("Goodbye!"); running = false; }
                default -> System.out.println("Invalid choice.");
            }
            System.out.println();
        }
    }

    private static void printMenu() {
        System.out.println("""
                1. Add book       5. Borrowed count
                2. Remove book    6. Mark borrowed
                3. List books     7. Mark returned
                4. Search title   8. Catalog summary
                0. Exit""");
    }

    private static void addBook() {
        String title = readLine("Title: ");
        String author = readLine("Author: ");
        library.addBook(new Book(title, author));
        System.out.println("Added.");
    }

    private static void removeBook() {
        boolean removed = library.removeBook(readLine("Title to remove: "));
        System.out.println(removed ? "Removed." : "Not found.");
    }

    private static void listBooks() {
        List<Book> books = library.listBooks();
        if (books.isEmpty()) {
            System.out.println("No books in the library.");
            return;
        }
        for (int i = 0; i < books.size(); i++) {
            System.out.println((i + 1) + ". " + books.get(i));
        }
    }

    private static void searchBook() {
        Optional<Book> result = library.searchByTitle(readLine("Title to search: "));
        System.out.println(result.isPresent() ? "Found: " + result.get() : "Not found.");
    }

    private static void setBorrowed(boolean borrowed) {
        Optional<Book> result = library.searchByTitle(readLine("Title: "));
        if (result.isEmpty()) {
            System.out.println("Not found.");
            return;
        }
        result.get().setBorrowed(borrowed);
        System.out.println((borrowed ? "Marked borrowed: " : "Marked returned: ") + result.get());
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Enter a number.");
            }
        }
    }
}