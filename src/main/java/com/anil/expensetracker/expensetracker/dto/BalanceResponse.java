package com.anil.expensetracker.expensetracker.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BalanceResponse {
    private Long userId;
    private String username;
    private BigDecimal totalOwed;
    private BigDecimal totalOwedTo;
    private BigDecimal netBalance;
    private String currency;
    private List<BalanceDetail> details;
    
    @Data
    public static class BalanceDetail {
        private Long otherUserId;
        private String otherUsername;
        private BigDecimal amount;
        private String type; // "owes" or "owed_by"
    }
}
