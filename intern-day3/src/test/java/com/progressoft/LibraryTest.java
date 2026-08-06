package com.progressoft;

import com.progressoft.model.Book;
import com.progressoft.service.Library;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class LibraryTest {
    private Library library;

    @Before
    public void setUp() {
        library = new Library();
    }

    @Test
    public void addingBookIncreasesListSize() {
        library.addBook(new Book("Dune", "Frank Herbert"));
        assertEquals(1, library.listBooks().size());
    }

    @Test
    public void searchFindsExistingTitleCaseInsensitive() {
        library.addBook(new Book("The Hobbit", "J.R.R. Tolkien"));

        Optional<Book> result = library.searchByTitle("the hobbit");

        assertTrue(result.isPresent());
        assertEquals("J.R.R. Tolkien", result.get().getAuthor());
    }

    @Test
    public void searchForMissingTitleReturnsEmptyNotCrash() {
        Optional<Book> result = library.searchByTitle("Nonexistent Book");
        assertFalse(result.isPresent());
    }

    @Test
    public void borrowedCountOnEmptyLibraryReturnsZero() {
        assertEquals(0, library.borrowedCount());
    }

    @Test
    public void removingExistingTitleRemovesItAndReturnsTrue() {
        library.addBook(new Book("The Hobbit", "J.R.R. Tolkien"));

        boolean removed = library.removeBook("The Hobbit");

        assertTrue(removed);
        assertTrue(library.listBooks().isEmpty());
    }
}