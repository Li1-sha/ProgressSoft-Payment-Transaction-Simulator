package com.progressoft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Library {
    private final List<Book> books;

    public Library() {
        this.books = new ArrayList<>();
    }

    public void addBook(Book book) {
        books.add(book);
    }

    public boolean removeBook(String title) {
        return books.removeIf(book -> book.getTitle().equalsIgnoreCase(title));
    }

    public List<Book> listBooks() {
        return new ArrayList<>(books); // Return a copy
    }

    public Optional<Book> searchByTitle(String title) {
        return books.stream()
                .filter(book -> book.getTitle().equalsIgnoreCase(title))
                .findFirst();
    }

    public int borrowedCount() {
        return (int) books.stream()
                .filter(Book::isBorrowed)
                .count();
    }

    public String catalogSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Library Catalog ===\n");
        if (books.isEmpty()) {
            sb.append("No books in the library.\n");
        } else {
            for (int i = 0; i < books.size(); i++) {
                sb.append(i + 1).append(". ").append(books.get(i).toString());
                if (i < books.size() - 1) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }
}