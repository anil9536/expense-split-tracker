package com.anil.expensetracker.expensetracker.dto;

import com.anil.expensetracker.expensetracker.model.Expense;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ExpenseRequest {
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    private String currency = "USD";
    
    @NotNull(message = "Split type is required")
    private Expense.SplitType splitType;
    
    @NotNull(message = "Group ID is required")
    private Long groupId;
    
    @NotNull(message = "Paid by user ID is required")
    private Long paidByUserId;
    
    // For equal split - list of user IDs
    private List<Long> userIds;
    
    // For exact amount split - map of user ID to amount
    private Map<Long, BigDecimal> exactAmounts;
    
    // For percentage split - map of user ID to percentage
    private Map<Long, BigDecimal> percentages;
}
