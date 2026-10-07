package com.interviewprep.expenses;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(Long id, BigDecimal amount, Category category, LocalDate date, String note) {

    static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(), expense.getAmount(), expense.getCategory(), expense.getDate(), expense.getNote());
    }
}
