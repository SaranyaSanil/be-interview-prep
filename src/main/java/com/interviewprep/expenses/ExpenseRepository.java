package com.interviewprep.expenses;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    // Summed in the database; NUMERIC SUM is exact, so no rounding drift.
    @Query("""
            SELECT new com.interviewprep.expenses.CategoryTotal(e.category, SUM(e.amount))
            FROM Expense e
            WHERE e.date BETWEEN :from AND :to
            GROUP BY e.category
            """)
    List<CategoryTotal> totalsByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
