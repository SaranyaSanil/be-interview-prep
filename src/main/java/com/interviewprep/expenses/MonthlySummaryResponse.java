package com.interviewprep.expenses;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

/** Totals for every category (0.00 when there is no spending) plus the overall total. */
public record MonthlySummaryResponse(YearMonth month, Map<Category, BigDecimal> totals, BigDecimal total) {
}
