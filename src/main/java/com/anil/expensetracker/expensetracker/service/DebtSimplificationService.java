package com.anil.expensetracker.expensetracker.service;

import com.anil.expensetracker.expensetracker.model.*;
import com.anil.expensetracker.expensetracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class DebtSimplificationService {
    
    private final ExpenseShareRepository expenseShareRepository;
    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    
    public List<Settlement> simplifyDebts(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        
        // Get all active members
        List<GroupMember> members = groupMemberRepository.findByGroupIdAndIsActiveTrue(groupId);
        
        // Calculate net balances for each user
        Map<Long, BigDecimal> netBalances = calculateNetBalances(groupId, members);
        
        // Separate creditors and debtors
        List<UserBalance> creditors = new ArrayList<>();
        List<UserBalance> debtors = new ArrayList<>();
        
        for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
            Long userId = entry.getKey();
            BigDecimal balance = entry.getValue();
            
            User user = members.stream()
                    .filter(m -> m.getUser().getId().equals(userId))
                    .map(GroupMember::getUser)
                    .findFirst()
                    .orElseThrow();
            
            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                debtors.add(new UserBalance(user, balance));
            } else if (balance.compareTo(BigDecimal.ZERO) < 0) {
                creditors.add(new UserBalance(user, balance.abs()));
            }
        }
        
        // Sort by amount (largest first)
        creditors.sort((a, b) -> b.balance.compareTo(a.balance));
        debtors.sort((a, b) -> b.balance.compareTo(a.balance));
        
        // Generate simplified settlements
        List<Settlement> settlements = new ArrayList<>();
        int creditorIndex = 0;
        int debtorIndex = 0;
        
        while (creditorIndex < creditors.size() && debtorIndex < debtors.size()) {
            UserBalance creditor = creditors.get(creditorIndex);
            UserBalance debtor = debtors.get(debtorIndex);
            
            BigDecimal settlementAmount = creditor.balance.min(debtor.balance);
            
            if (settlementAmount.compareTo(BigDecimal.ZERO) > 0) {
                Settlement settlement = new Settlement();
                settlement.setGroup(group);
                settlement.setPayer(debtor.user);
                settlement.setReceiver(creditor.user);
                settlement.setAmount(settlementAmount);
                settlement.setCurrency("USD");
                settlement.setNotes("Simplified debt settlement");
                
                settlements.add(settlement);
                
                // Update balances
                creditor.balance = creditor.balance.subtract(settlementAmount);
                debtor.balance = debtor.balance.subtract(settlementAmount);
                
                // Mark corresponding expense shares as settled
                markExpenseSharesAsSettled(groupId, debtor.user.getId(), creditor.user.getId(), settlementAmount);
            }
            
            // Move to next creditor/debtor if current one is fully settled
            if (creditor.balance.compareTo(BigDecimal.ZERO) <= 0) {
                creditorIndex++;
            }
            if (debtor.balance.compareTo(BigDecimal.ZERO) <= 0) {
                debtorIndex++;
            }
        }
        
        // Save all settlements
        return settlementRepository.saveAll(settlements);
    }
    
    private Map<Long, BigDecimal> calculateNetBalances(Long groupId, List<GroupMember> members) {
        Map<Long, BigDecimal> netBalances = new HashMap<>();
        
        for (GroupMember member : members) {
            Long userId = member.getUser().getId();
            
            // Get total amount user owes (from expense shares)
            BigDecimal totalOwed = expenseShareRepository.getTotalOwedByUserInGroup(userId, groupId);
            
            // Get total amount owed to user (from expenses they paid)
            BigDecimal totalOwedTo = expenseShareRepository.getTotalOwedToUserInGroup(userId, groupId);
            
            // Calculate net balance (positive means user owes money, negative means user is owed money)
            BigDecimal netBalance = totalOwed.subtract(totalOwedTo);
            netBalances.put(userId, netBalance);
        }
        
        return netBalances;
    }
    
    private void markExpenseSharesAsSettled(Long groupId, Long payerId, Long receiverId, BigDecimal settlementAmount) {
        List<ExpenseShare> shares = expenseShareRepository.findByUserIdAndGroupIdAndIsSettledFalse(payerId, groupId);
        
        BigDecimal remainingAmount = settlementAmount;
        
        for (ExpenseShare share : shares) {
            if (share.getExpense().getPaidBy().getId().equals(receiverId) && remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (remainingAmount.compareTo(share.getShareAmount()) >= 0) {
                    // Fully settle this share
                    share.setIsSettled(true);
                    remainingAmount = remainingAmount.subtract(share.getShareAmount());
                } else {
                    // Partially settle this share
                    share.setShareAmount(share.getShareAmount().subtract(remainingAmount));
                    remainingAmount = BigDecimal.ZERO;
                }
                expenseShareRepository.save(share);
            }
        }
    }
    
    private static class UserBalance {
        User user;
        BigDecimal balance;
        
        UserBalance(User user, BigDecimal balance) {
            this.user = user;
            this.balance = balance;
        }
    }
}
