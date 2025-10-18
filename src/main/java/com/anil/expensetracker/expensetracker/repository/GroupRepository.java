package com.anil.expensetracker.expensetracker.repository;

import com.anil.expensetracker.expensetracker.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT g FROM Group g JOIN g.members gm WHERE gm.user.id = :userId AND gm.isActive = true")
    List<Group> findByUserId(@Param("userId") Long userId);
}
