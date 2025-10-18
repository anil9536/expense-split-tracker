package com.anil.expensetracker.expensetracker.repository;

import com.anil.expensetracker.expensetracker.model.ExpenseShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {
    List<ExpenseShare> findByUserIdAndIsSettledFalse(Long userId);
    
    @Query("SELECT es FROM ExpenseShare es WHERE es.user.id = :userId AND es.expense.group.id = :groupId AND es.isSettled = false")
    List<ExpenseShare> findByUserIdAndGroupIdAndIsSettledFalse(@Param("userId") Long userId, @Param("groupId") Long groupId);
    
    @Query("SELECT COALESCE(SUM(es.shareAmount), 0) FROM ExpenseShare es WHERE es.user.id = :userId AND es.expense.group.id = :groupId AND es.isSettled = false")
    BigDecimal getTotalOwedByUserInGroup(@Param("userId") Long userId, @Param("groupId") Long groupId);
    
    @Query("SELECT COALESCE(SUM(es.shareAmount), 0) FROM ExpenseShare es WHERE es.expense.paidBy.id = :userId AND es.expense.group.id = :groupId AND es.isSettled = false")
    BigDecimal getTotalOwedToUserInGroup(@Param("userId") Long userId, @Param("groupId") Long groupId);
}
