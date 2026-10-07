package com.interviewprep.expenses;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        // Matches NUMERIC(12,2); a third decimal place is rejected rather than silently rounded.
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull Category category,
        @NotNull LocalDate date,
        @Size(max = 500) String note) {
}
