package com.anil.expensetracker.expensetracker;

import com.anil.expensetracker.expensetracker.dto.*;
import com.anil.expensetracker.expensetracker.model.*;
import com.anil.expensetracker.expensetracker.repository.*;
import com.anil.expensetracker.expensetracker.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ExpenseTrackerIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private GroupService groupService;
    
    @Autowired
    private ExpenseService expenseService;
    
    @Autowired
    private BalanceService balanceService;
    
    @Autowired
    private DebtSimplificationService debtSimplificationService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private SettlementRepository settlementRepository;
    
    private User user1, user2, user3;
    private Group group;
    
    @BeforeEach
    void setUp() {
        // Clean up existing data
        settlementRepository.deleteAll();
        expenseRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create test users
        UserRequest userRequest1 = new UserRequest();
        userRequest1.setUsername("user1");
        userRequest1.setEmail("user1@test.com");
        userRequest1.setName("User One");
        user1 = userService.createUser(userRequest1);
        
        UserRequest userRequest2 = new UserRequest();
        userRequest2.setUsername("user2");
        userRequest2.setEmail("user2@test.com");
        userRequest2.setName("User Two");
        user2 = userService.createUser(userRequest2);
        
        UserRequest userRequest3 = new UserRequest();
        userRequest3.setUsername("user3");
        userRequest3.setEmail("user3@test.com");
        userRequest3.setName("User Three");
        user3 = userService.createUser(userRequest3);
        
        // Create test group
        GroupRequest groupRequest = new GroupRequest();
        groupRequest.setName("Trip to Paris");
        groupRequest.setDescription("Weekend trip to Paris");
        group = groupService.createGroup(groupRequest, user1.getId());
        
        // Add all users to group
        groupService.addMemberToGroup(group.getId(), user2.getId());
        groupService.addMemberToGroup(group.getId(), user3.getId());
    }
    
    @Test
    void testEqualSplitExpense() {
        // Test Case 1: Equal Split of Expense
        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setDescription("Dinner");
        expenseRequest.setAmount(new BigDecimal("90.00"));
        expenseRequest.setCurrency("USD");
        expenseRequest.setSplitType(Expense.SplitType.EQUAL);
        expenseRequest.setGroupId(group.getId());
        expenseRequest.setPaidByUserId(user1.getId());
        expenseRequest.setUserIds(List.of(user1.getId(), user2.getId(), user3.getId()));
        
        Expense expense = expenseService.createExpense(expenseRequest);
        assertNotNull(expense);
        
        // Check balances
        BalanceResponse balance1 = balanceService.getUserBalanceInGroup(group.getId(), user1.getId());
        BalanceResponse balance2 = balanceService.getUserBalanceInGroup(group.getId(), user2.getId());
        BalanceResponse balance3 = balanceService.getUserBalanceInGroup(group.getId(), user3.getId());
        
        // User1 paid $90, owes $30, net balance should be -$60 (owed $60)
        assertEquals(new BigDecimal("30.00"), balance1.getTotalOwed());
        assertEquals(new BigDecimal("60.00"), balance1.getTotalOwedTo());
        assertEquals(new BigDecimal("-30.00"), balance1.getNetBalance());
        
        // User2 owes $30
        assertEquals(new BigDecimal("30.00"), balance2.getTotalOwed());
        assertEquals(new BigDecimal("0.00"), balance2.getTotalOwedTo());
        assertEquals(new BigDecimal("30.00"), balance2.getNetBalance());
        
        // User3 owes $30
        assertEquals(new BigDecimal("30.00"), balance3.getTotalOwed());
        assertEquals(new BigDecimal("0.00"), balance3.getTotalOwedTo());
        assertEquals(new BigDecimal("30.00"), balance3.getNetBalance());
    }
    
    @Test
    void testExactAmountSplit() {
        // Test Case 2: Exact Amount Split
        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setDescription("Shopping");
        expenseRequest.setAmount(new BigDecimal("100.00"));
        expenseRequest.setCurrency("USD");
        expenseRequest.setSplitType(Expense.SplitType.EXACT_AMOUNT);
        expenseRequest.setGroupId(group.getId());
        expenseRequest.setPaidByUserId(user1.getId());
        
        Map<Long, BigDecimal> exactAmounts = new HashMap<>();
        exactAmounts.put(user1.getId(), new BigDecimal("70.00"));
        exactAmounts.put(user2.getId(), new BigDecimal("30.00"));
        expenseRequest.setExactAmounts(exactAmounts);
        
        Expense expense = expenseService.createExpense(expenseRequest);
        assertNotNull(expense);
        
        // Check balances
        BalanceResponse balance1 = balanceService.getUserBalanceInGroup(group.getId(), user1.getId());
        BalanceResponse balance2 = balanceService.getUserBalanceInGroup(group.getId(), user2.getId());
        
        // User1 paid $100, owes $70, net balance should be -$30 (owed $30)
        assertEquals(new BigDecimal("70.00"), balance1.getTotalOwed());
        assertEquals(new BigDecimal("30.00"), balance1.getTotalOwedTo());
        assertEquals(new BigDecimal("-30.00"), balance1.getNetBalance());
        
        // User2 owes $30
        assertEquals(new BigDecimal("30.00"), balance2.getTotalOwed());
        assertEquals(new BigDecimal("0.00"), balance2.getTotalOwedTo());
        assertEquals(new BigDecimal("30.00"), balance2.getNetBalance());
    }
    
    @Test
    void testPercentageSplit() {
        // Test Case 3: Percentage Split
        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setDescription("Hotel");
        expenseRequest.setAmount(new BigDecimal("200.00"));
        expenseRequest.setCurrency("USD");
        expenseRequest.setSplitType(Expense.SplitType.PERCENTAGE);
        expenseRequest.setGroupId(group.getId());
        expenseRequest.setPaidByUserId(user1.getId());
        
        Map<Long, BigDecimal> percentages = new HashMap<>();
        percentages.put(user1.getId(), new BigDecimal("60"));
        percentages.put(user2.getId(), new BigDecimal("40"));
        expenseRequest.setPercentages(percentages);
        
        Expense expense = expenseService.createExpense(expenseRequest);
        assertNotNull(expense);
        
        // Check balances
        BalanceResponse balance1 = balanceService.getUserBalanceInGroup(group.getId(), user1.getId());
        BalanceResponse balance2 = balanceService.getUserBalanceInGroup(group.getId(), user2.getId());
        
        // User1 paid $200, owes $120 (60%), net balance should be -$80 (owed $80)
        assertEquals(new BigDecimal("120.00"), balance1.getTotalOwed());
        assertEquals(new BigDecimal("80.00"), balance1.getTotalOwedTo());
        assertEquals(new BigDecimal("-40.00"), balance1.getNetBalance());
        
        // User2 owes $80 (40%)
        assertEquals(new BigDecimal("80.00"), balance2.getTotalOwed());
        assertEquals(new BigDecimal("0.00"), balance2.getTotalOwedTo());
        assertEquals(new BigDecimal("80.00"), balance2.getNetBalance());
    }
    
    @Test
    void testSettlingDebt() {
        // First create an expense
        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setDescription("Taxi");
        expenseRequest.setAmount(new BigDecimal("50.00"));
        expenseRequest.setCurrency("USD");
        expenseRequest.setSplitType(Expense.SplitType.EQUAL);
        expenseRequest.setGroupId(group.getId());
        expenseRequest.setPaidByUserId(user1.getId());
        expenseRequest.setUserIds(List.of(user1.getId(), user2.getId()));
        
        expenseService.createExpense(expenseRequest);
        
        // User2 owes $25 to User1
        BalanceResponse balanceBefore = balanceService.getUserBalanceInGroup(group.getId(), user2.getId());
        assertEquals(new BigDecimal("25.00"), balanceBefore.getTotalOwed());
        
        // Settle the debt
        SettlementRequest settlementRequest = new SettlementRequest();
        settlementRequest.setGroupId(group.getId());
        settlementRequest.setPayerId(user2.getId());
        settlementRequest.setReceiverId(user1.getId());
        settlementRequest.setAmount(new BigDecimal("25.00"));
        settlementRequest.setCurrency("USD");
        settlementRequest.setNotes("Taxi fare settlement");
        
        Settlement settlement = balanceService.createSettlement(settlementRequest);
        assertNotNull(settlement);
        
        // Check balance after settlement
        BalanceResponse balanceAfter = balanceService.getUserBalanceInGroup(group.getId(), user2.getId());
        assertEquals(new BigDecimal("0.00"), balanceAfter.getTotalOwed());
        assertEquals(new BigDecimal("0.00"), balanceAfter.getTotalOwedTo());
        assertEquals(new BigDecimal("0.00"), balanceAfter.getNetBalance());
    }
    
    @Test
    void testSimplifyDebts() {
        // Create multiple expenses to create complex debt structure
        // User1 pays $30, User2 pays $20
        ExpenseRequest expense1 = new ExpenseRequest();
        expense1.setDescription("Expense 1");
        expense1.setAmount(new BigDecimal("30.00"));
        expense1.setCurrency("USD");
        expense1.setSplitType(Expense.SplitType.EXACT_AMOUNT);
        expense1.setGroupId(group.getId());
        expense1.setPaidByUserId(user1.getId());
        
        Map<Long, BigDecimal> amounts1 = new HashMap<>();
        amounts1.put(user1.getId(), new BigDecimal("30.00"));
        expense1.setExactAmounts(amounts1);
        expenseService.createExpense(expense1);
        
        ExpenseRequest expense2 = new ExpenseRequest();
        expense2.setDescription("Expense 2");
        expense2.setAmount(new BigDecimal("20.00"));
        expense2.setCurrency("USD");
        expense2.setSplitType(Expense.SplitType.EXACT_AMOUNT);
        expense2.setGroupId(group.getId());
        expense2.setPaidByUserId(user2.getId());
        
        Map<Long, BigDecimal> amounts2 = new HashMap<>();
        amounts2.put(user2.getId(), new BigDecimal("20.00"));
        expense2.setExactAmounts(amounts2);
        expenseService.createExpense(expense2);
        
        // Check balances before simplification
        BalanceResponse balance1Before = balanceService.getUserBalanceInGroup(group.getId(), user1.getId());
        BalanceResponse balance2Before = balanceService.getUserBalanceInGroup(group.getId(), user2.getId());
        
        // User1 owes $30, User2 owes $20
        assertEquals(new BigDecimal("30.00"), balance1Before.getTotalOwed());
        assertEquals(new BigDecimal("0.00"), balance1Before.getTotalOwedTo());
        assertEquals(new BigDecimal("30.00"), balance1Before.getNetBalance());
        
        assertEquals(new BigDecimal("20.00"), balance2Before.getTotalOwed());
        assertEquals(new BigDecimal("0.00"), balance2Before.getTotalOwedTo());
        assertEquals(new BigDecimal("20.00"), balance2Before.getNetBalance());
        
        // Simplify debts
        List<Settlement> settlements = debtSimplificationService.simplifyDebts(group.getId());
        assertNotNull(settlements);
        assertFalse(settlements.isEmpty());
        
        // Check balances after simplification
        BalanceResponse balance1After = balanceService.getUserBalanceInGroup(group.getId(), user1.getId());
        BalanceResponse balance2After = balanceService.getUserBalanceInGroup(group.getId(), user2.getId());
        
        // After simplification, User1 should owe $10 and User2 should owe $20 to User1
        assertEquals(new BigDecimal("10.00"), balance1After.getTotalOwed());
        assertEquals(new BigDecimal("20.00"), balance1After.getTotalOwedTo());
        assertEquals(new BigDecimal("-10.00"), balance1After.getNetBalance());
        
        assertEquals(new BigDecimal("20.00"), balance2After.getTotalOwed());
        assertEquals(new BigDecimal("0.00"), balance2After.getTotalOwedTo());
        assertEquals(new BigDecimal("20.00"), balance2After.getNetBalance());
    }
    
    @Test
    void testInvalidSettlement() {
        // Create an expense
        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setDescription("Test Expense");
        expenseRequest.setAmount(new BigDecimal("50.00"));
        expenseRequest.setCurrency("USD");
        expenseRequest.setSplitType(Expense.SplitType.EQUAL);
        expenseRequest.setGroupId(group.getId());
        expenseRequest.setPaidByUserId(user1.getId());
        expenseRequest.setUserIds(List.of(user1.getId(), user2.getId()));
        
        expenseService.createExpense(expenseRequest);
        
        // Try to settle more than owed
        SettlementRequest settlementRequest = new SettlementRequest();
        settlementRequest.setGroupId(group.getId());
        settlementRequest.setPayerId(user2.getId());
        settlementRequest.setReceiverId(user1.getId());
        settlementRequest.setAmount(new BigDecimal("100.00")); // More than the $25 owed
        settlementRequest.setCurrency("USD");
        
        assertThrows(IllegalArgumentException.class, () -> {
            balanceService.createSettlement(settlementRequest);
        });
    }
}
