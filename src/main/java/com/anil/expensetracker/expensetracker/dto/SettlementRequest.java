package com.anil.expensetracker.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettlementRequest {
    @NotNull(message = "Group ID is required")
    private Long groupId;
    
    @NotNull(message = "Payer ID is required")
    private Long payerId;
    
    @NotNull(message = "Receiver ID is required")
    private Long receiverId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    private String currency = "USD";
    
    private String notes;
}
