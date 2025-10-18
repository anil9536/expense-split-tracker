package com.anil.expensetracker.expensetracker.repository;

import com.anil.expensetracker.expensetracker.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    
    List<GroupMember> findByGroupIdAndIsActiveTrue(Long groupId);
    
    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.id = :userId AND gm.isActive = true")
    Optional<GroupMember> findActiveMemberByGroupAndUser(@Param("groupId") Long groupId, @Param("userId") Long userId);
    
    boolean existsByGroupIdAndUserIdAndIsActiveTrue(Long groupId, Long userId);
}
