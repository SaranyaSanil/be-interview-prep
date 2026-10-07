package com.interviewprep.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.interviewprep.common.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseService expenseService;

    @Test
    void summaryQueriesFromFirstToLastDayOfMonth() {
        when(expenseRepository.totalsByCategory(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29)))
                .thenReturn(List.of());

        MonthlySummaryResponse summary = expenseService.monthlySummary(YearMonth.of(2028, 2));

        assertThat(summary.total()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void summaryIncludesEveryCategoryAndAddsTotalsExactly() {
        when(expenseRepository.totalsByCategory(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(
                        new CategoryTotal(Category.FOOD, new BigDecimal("0.10")),
                        new CategoryTotal(Category.BILLS, new BigDecimal("0.20"))));

        MonthlySummaryResponse summary = expenseService.monthlySummary(YearMonth.of(2026, 1));

        assertThat(summary.totals()).containsOnlyKeys(Category.values());
        assertThat(summary.totals().get(Category.TRAVEL)).isEqualTo(new BigDecimal("0.00"));
        // equals (not compareTo) also checks the scale: exactly 0.30, not 0.3 or 0.30000000000000004.
        assertThat(summary.total()).isEqualTo(new BigDecimal("0.30"));
    }

    @Test
    void listRejectsFromAfterTo() {
        assertThatThrownBy(() -> expenseService.list(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(BadRequestException.class);
    }
}
