package com.anil.expensetracker.expensetracker.service;

import com.anil.expensetracker.expensetracker.dto.BalanceResponse;
import com.anil.expensetracker.expensetracker.dto.SettlementRequest;
import com.anil.expensetracker.expensetracker.model.*;
import com.anil.expensetracker.expensetracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BalanceService {
    
    private final ExpenseShareRepository expenseShareRepository;
    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    
    public BalanceResponse getUserBalanceInGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (!groupMemberRepository.existsByGroupIdAndUserIdAndIsActiveTrue(groupId, userId)) {
            throw new IllegalArgumentException("User is not a member of this group");
        }
        
        // Get total amount user owes (from expense shares)
        BigDecimal totalOwed = expenseShareRepository.getTotalOwedByUserInGroup(userId, groupId);
        
        // Get total amount owed to user (from expenses they paid)
        BigDecimal totalOwedTo = expenseShareRepository.getTotalOwedToUserInGroup(userId, groupId);
        
        // Calculate net balance (positive means user owes money, negative means user is owed money)
        BigDecimal netBalance = totalOwed.subtract(totalOwedTo);
        
        BalanceResponse response = new BalanceResponse();
        response.setUserId(userId);
        response.setUsername(user.getUsername());
        response.setTotalOwed(totalOwed);
        response.setTotalOwedTo(totalOwedTo);
        response.setNetBalance(netBalance);
        response.setCurrency("USD"); // Default currency
        
        // Get detailed balances with other users
        List<BalanceResponse.BalanceDetail> details = getDetailedBalances(groupId, userId);
        response.setDetails(details);
        
        return response;
    }
    
    private List<BalanceResponse.BalanceDetail> getDetailedBalances(Long groupId, Long userId) {
        List<BalanceResponse.BalanceDetail> details = new ArrayList<>();
        
        // Get all active members in the group
        List<GroupMember> members = groupMemberRepository.findByGroupIdAndIsActiveTrue(groupId);
        
        for (GroupMember member : members) {
            if (member.getUser().getId().equals(userId)) {
                continue; // Skip self
            }
            
            Long otherUserId = member.getUser().getId();
            
            // Calculate what current user owes to this other user
            BigDecimal owesToOther = calculateAmountOwedBetweenUsers(groupId, userId, otherUserId);
            
            // Calculate what this other user owes to current user
            BigDecimal otherOwesToCurrent = calculateAmountOwedBetweenUsers(groupId, otherUserId, userId);
            
            BigDecimal netAmount = owesToOther.subtract(otherOwesToCurrent);
            
            if (netAmount.compareTo(BigDecimal.ZERO) != 0) {
                BalanceResponse.BalanceDetail detail = new BalanceResponse.BalanceDetail();
                detail.setOtherUserId(otherUserId);
                detail.setOtherUsername(member.getUser().getUsername());
                detail.setAmount(netAmount.abs());
                detail.setType(netAmount.compareTo(BigDecimal.ZERO) > 0 ? "owes" : "owed_by");
                details.add(detail);
            }
        }
        
        return details;
    }
    
    private BigDecimal calculateAmountOwedBetweenUsers(Long groupId, Long debtorId, Long creditorId) {
        // Get all expense shares where debtor owes money
        List<ExpenseShare> debtorShares = expenseShareRepository.findByUserIdAndGroupIdAndIsSettledFalse(debtorId, groupId);
        
        BigDecimal totalOwed = BigDecimal.ZERO;
        for (ExpenseShare share : debtorShares) {
            if (share.getExpense().getPaidBy().getId().equals(creditorId)) {
                totalOwed = totalOwed.add(share.getShareAmount());
            }
        }
        
        return totalOwed;
    }
    
    public Settlement createSettlement(SettlementRequest request) {
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        
        User payer = userRepository.findById(request.getPayerId())
                .orElseThrow(() -> new IllegalArgumentException("Payer not found"));
        
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));
        
        if (payer.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Payer and receiver cannot be the same person");
        }
        
        // Verify both users are members of the group
        if (!groupMemberRepository.existsByGroupIdAndUserIdAndIsActiveTrue(request.getGroupId(), request.getPayerId()) ||
            !groupMemberRepository.existsByGroupIdAndUserIdAndIsActiveTrue(request.getGroupId(), request.getReceiverId())) {
            throw new IllegalArgumentException("Both users must be members of the group");
        }
        
        // Check if payer actually owes money to receiver
        BigDecimal amountOwed = calculateAmountOwedBetweenUsers(request.getGroupId(), request.getPayerId(), request.getReceiverId());
        
        if (amountOwed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payer does not owe any money to receiver");
        }
        
        if (request.getAmount().compareTo(amountOwed) > 0) {
            throw new IllegalArgumentException("Settlement amount cannot exceed the amount owed");
        }
        
        Settlement settlement = new Settlement();
        settlement.setGroup(group);
        settlement.setPayer(payer);
        settlement.setReceiver(receiver);
        settlement.setAmount(request.getAmount());
        settlement.setCurrency(request.getCurrency());
        settlement.setNotes(request.getNotes());
        
        Settlement savedSettlement = settlementRepository.save(settlement);
        
        // Mark corresponding expense shares as settled
        markExpenseSharesAsSettled(request.getGroupId(), request.getPayerId(), request.getReceiverId(), request.getAmount());
        
        return savedSettlement;
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
    
    public List<Settlement> getSettlementsByGroup(Long groupId) {
        return settlementRepository.findByGroupIdOrderBySettledAtDesc(groupId);
    }
    
    public List<Settlement> getSettlementsByUserInGroup(Long groupId, Long userId) {
        return settlementRepository.findByGroupIdAndUserId(groupId, userId);
    }
}
