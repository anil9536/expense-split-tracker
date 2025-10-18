package com.anil.expensetracker.expensetracker.repository;

import com.anil.expensetracker.expensetracker.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroupIdOrderByCreatedAtDesc(Long groupId);
    
    @Query("SELECT e FROM Expense e WHERE e.group.id = :groupId AND e.paidBy.id = :userId ORDER BY e.createdAt DESC")
    List<Expense> findByGroupIdAndPaidByUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
