package com.interviewprep.library;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, unique = true)
    private String isbn;

    private Integer publishedYear;

    private String borrowedBy;

    private Instant borrowedAt;

    // Optimistic locking: concurrent updates to the same book fail instead of overwriting each other.
    @Version
    private Long version;

    protected Book() {
    }

    public Book(String title, String author, String isbn, Integer publishedYear) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publishedYear = publishedYear;
    }

    public boolean isBorrowed() {
        return borrowedBy != null;
    }

    public void borrow(String memberId, Instant at) {
        this.borrowedBy = memberId;
        this.borrowedAt = at;
    }

    public void giveBack() {
        this.borrowedBy = null;
        this.borrowedAt = null;
    }

    public void updateDetails(String title, String author, String isbn, Integer publishedYear) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publishedYear = publishedYear;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public Integer getPublishedYear() {
        return publishedYear;
    }

    public String getBorrowedBy() {
        return borrowedBy;
    }

    public Instant getBorrowedAt() {
        return borrowedAt;
    }
}
