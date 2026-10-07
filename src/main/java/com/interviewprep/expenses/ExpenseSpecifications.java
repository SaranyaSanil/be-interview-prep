package com.interviewprep.expenses;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/** Optional list filters; a null argument means "no filter" and adds no condition. */
final class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    static Specification<Expense> onOrAfter(LocalDate from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("date"), from);
    }

    static Specification<Expense> onOrBefore(LocalDate to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("date"), to);
    }

    static Specification<Expense> inCategory(Category category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }
}
