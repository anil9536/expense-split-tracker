package com.anil.expensetracker.expensetracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "expense_shares")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseShare {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull(message = "Share amount is required")
    @DecimalMin(value = "0.01", message = "Share amount must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal shareAmount;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;
    
    @Column(name = "is_settled")
    private Boolean isSettled = false;
}
