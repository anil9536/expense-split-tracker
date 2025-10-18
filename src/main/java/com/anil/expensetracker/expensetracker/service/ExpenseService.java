package com.anil.expensetracker.expensetracker.service;

import com.anil.expensetracker.expensetracker.dto.ExpenseRequest;
import com.anil.expensetracker.expensetracker.model.*;
import com.anil.expensetracker.expensetracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {
    
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    
    public Expense createExpense(ExpenseRequest request) {
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        
        User paidBy = userRepository.findById(request.getPaidByUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Verify that the user who paid is a member of the group
        if (!groupMemberRepository.existsByGroupIdAndUserIdAndIsActiveTrue(request.getGroupId(), request.getPaidByUserId())) {
            throw new IllegalArgumentException("User is not a member of this group");
        }
        
        Expense expense = new Expense();
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setCurrency(request.getCurrency());
        expense.setSplitType(request.getSplitType());
        expense.setGroup(group);
        expense.setPaidBy(paidBy);
        
        Expense savedExpense = expenseRepository.save(expense);
        
        // Create expense shares based on split type
        createExpenseShares(savedExpense, request);
        
        return savedExpense;
    }
    
    private void createExpenseShares(Expense expense, ExpenseRequest request) {
        List<ExpenseShare> shares = new ArrayList<>();
        
        switch (request.getSplitType()) {
            case EQUAL -> {
                if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
                    throw new IllegalArgumentException("User IDs are required for equal split");
                }
                
                BigDecimal shareAmount = expense.getAmount()
                        .divide(BigDecimal.valueOf(request.getUserIds().size()), 2, RoundingMode.HALF_UP);
                
                for (Long userId : request.getUserIds()) {
                    if (!groupMemberRepository.existsByGroupIdAndUserIdAndIsActiveTrue(expense.getGroup().getId(), userId)) {
                        throw new IllegalArgumentException("User " + userId + " is not a member of this group");
                    }
                    
                    ExpenseShare share = new ExpenseShare();
                    share.setExpense(expense);
                    share.setUser(userRepository.findById(userId).orElseThrow());
                    share.setShareAmount(shareAmount);
                    shares.add(share);
                }
            }
            case EXACT_AMOUNT -> {
                if (request.getExactAmounts() == null || request.getExactAmounts().isEmpty()) {
                    throw new IllegalArgumentException("Exact amounts are required for exact amount split");
                }
                
                BigDecimal totalAmount = request.getExactAmounts().values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (totalAmount.compareTo(expense.getAmount()) != 0) {
                    throw new IllegalArgumentException("Sum of exact amounts must equal the total expense amount");
                }
                
                for (Map.Entry<Long, BigDecimal> entry : request.getExactAmounts().entrySet()) {
                    Long userId = entry.getKey();
                    BigDecimal amount = entry.getValue();
                    
                    if (!groupMemberRepository.existsByGroupIdAndUserIdAndIsActiveTrue(expense.getGroup().getId(), userId)) {
                        throw new IllegalArgumentException("User " + userId + " is not a member of this group");
                    }
                    
                    ExpenseShare share = new ExpenseShare();
                    share.setExpense(expense);
                    share.setUser(userRepository.findById(userId).orElseThrow());
                    share.setShareAmount(amount);
                    shares.add(share);
                }
            }
            case PERCENTAGE -> {
                if (request.getPercentages() == null || request.getPercentages().isEmpty()) {
                    throw new IllegalArgumentException("Percentages are required for percentage split");
                }
                
                BigDecimal totalPercentage = request.getPercentages().values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0) {
                    throw new IllegalArgumentException("Sum of percentages must equal 100");
                }
                
                for (Map.Entry<Long, BigDecimal> entry : request.getPercentages().entrySet()) {
                    Long userId = entry.getKey();
                    BigDecimal percentage = entry.getValue();
                    
                    if (!groupMemberRepository.existsByGroupIdAndUserIdAndIsActiveTrue(expense.getGroup().getId(), userId)) {
                        throw new IllegalArgumentException("User " + userId + " is not a member of this group");
                    }
                    
                    BigDecimal shareAmount = expense.getAmount()
                            .multiply(percentage)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    
                    ExpenseShare share = new ExpenseShare();
                    share.setExpense(expense);
                    share.setUser(userRepository.findById(userId).orElseThrow());
                    share.setShareAmount(shareAmount);
                    share.setPercentage(percentage);
                    shares.add(share);
                }
            }
        }
        
        expenseShareRepository.saveAll(shares);
    }
    
    public List<Expense> getExpensesByGroup(Long groupId) {
        return expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }
    
    public List<Expense> getExpensesByUserInGroup(Long groupId, Long userId) {
        return expenseRepository.findByGroupIdAndPaidByUserId(groupId, userId);
    }
}
