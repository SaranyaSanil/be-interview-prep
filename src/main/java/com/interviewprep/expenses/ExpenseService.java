package com.interviewprep.expenses;

import static com.interviewprep.expenses.ExpenseSpecifications.inCategory;
import static com.interviewprep.expenses.ExpenseSpecifications.onOrAfter;
import static com.interviewprep.expenses.ExpenseSpecifications.onOrBefore;

import com.interviewprep.common.BadRequestException;
import com.interviewprep.common.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ExpenseService {

    private static final Sort BY_DATE = Sort.by("date", "id");
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    /** All filters are optional and combine with AND; both date bounds are inclusive. */
    public List<Expense> list(LocalDate from, LocalDate to, Category category) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' must not be after 'to'");
        }
        Specification<Expense> filter = Specification.allOf(onOrAfter(from), onOrBefore(to), inCategory(category));
        return expenseRepository.findAll(filter, BY_DATE);
    }

    public Expense get(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expense " + id + " not found"));
    }

    @Transactional
    public Expense create(ExpenseRequest request) {
        return expenseRepository.save(
                new Expense(request.amount(), request.category(), request.date(), normalizeNote(request.note())));
    }

    @Transactional
    public Expense update(Long id, ExpenseRequest request) {
        Expense expense = get(id);
        expense.update(request.amount(), request.category(), request.date(), normalizeNote(request.note()));
        return expense;
    }

    @Transactional
    public void delete(Long id) {
        expenseRepository.delete(get(id));
    }

    /**
     * The month runs from its first to its last calendar day, both inclusive. Dates are
     * LocalDate (no time part), so an expense on the 1st or the 31st is always inside.
     */
    public MonthlySummaryResponse monthlySummary(YearMonth month) {
        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            totals.put(category, ZERO);
        }
        expenseRepository.totalsByCategory(month.atDay(1), month.atEndOfMonth())
                .forEach(row -> totals.put(row.category(), row.total().setScale(2)));

        BigDecimal overall = totals.values().stream().reduce(ZERO, BigDecimal::add);
        return new MonthlySummaryResponse(month, totals, overall);
    }

    private static String normalizeNote(String note) {
        return StringUtils.hasText(note) ? note.trim() : null;
    }
}
