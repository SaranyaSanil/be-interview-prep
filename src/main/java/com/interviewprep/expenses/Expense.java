package com.interviewprep.expenses;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NUMERIC(12,2): exact decimal storage; never float/double for money.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // Stored as the name, so reordering the enum can't change existing rows.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    // A calendar date without time or zone, so month boundaries are exact.
    @Column(name = "expense_date", nullable = false)
    private LocalDate date;

    @Column(length = 500)
    private String note;

    protected Expense() {
    }

    public Expense(BigDecimal amount, Category category, LocalDate date, String note) {
        update(amount, category, date, note);
    }

    public void update(BigDecimal amount, Category category, LocalDate date, String note) {
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Category getCategory() {
        return category;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getNote() {
        return note;
    }
}
