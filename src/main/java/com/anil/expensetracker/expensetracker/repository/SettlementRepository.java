package com.anil.expensetracker.expensetracker.repository;

import com.anil.expensetracker.expensetracker.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByGroupIdOrderBySettledAtDesc(Long groupId);
    
    @Query("SELECT s FROM Settlement s WHERE s.group.id = :groupId AND (s.payer.id = :userId OR s.receiver.id = :userId) ORDER BY s.settledAt DESC")
    List<Settlement> findByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
