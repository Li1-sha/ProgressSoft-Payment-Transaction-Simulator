package com.progressoft.service;

import com.progressoft.model.Book;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Library {
    private final List<Book> books = new ArrayList<>();

    public void addBook(Book book) {
        books.add(book);
    }

    public boolean removeBook(String title) {
        return books.removeIf(book -> book.getTitle().equalsIgnoreCase(title));
    }

    public List<Book> listBooks() {
        return new ArrayList<>(books);
    }

    public Optional<Book> searchByTitle(String title) {
        return books.stream()
                .filter(book -> book.getTitle().equalsIgnoreCase(title))
                .findFirst();
    }

    public int borrowedCount() {
        return (int) books.stream().filter(Book::isBorrowed).count();
    }

    public String catalogSummary() {
        StringBuilder sb = new StringBuilder("=== Library Catalog ===\n");
        if (books.isEmpty()) {
            sb.append("No books in the library.");
            return sb.toString();
        }
        for (int i = 0; i < books.size(); i++) {
            sb.append(i + 1).append(". ").append(books.get(i));
            if (i < books.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }
}