package com.interviewprep.library;

import java.time.Instant;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        Integer publishedYear,
        boolean available,
        String borrowedBy,
        Instant borrowedAt) {

    static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublishedYear(),
                !book.isBorrowed(),
                book.getBorrowedBy(),
                book.getBorrowedAt());
    }
}
