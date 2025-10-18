package com.anil.expensetracker.expensetracker.controller;

import com.anil.expensetracker.expensetracker.dto.ExpenseRequest;
import com.anil.expensetracker.expensetracker.model.Expense;
import com.anil.expensetracker.expensetracker.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    
    private final ExpenseService expenseService;
    
    @PostMapping
    public ResponseEntity<Expense> createExpense(@Valid @RequestBody ExpenseRequest request) {
        try {
            Expense expense = expenseService.createExpense(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(expense);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Expense>> getExpensesByGroup(@PathVariable Long groupId) {
        List<Expense> expenses = expenseService.getExpensesByGroup(groupId);
        return ResponseEntity.ok(expenses);
    }
    
    @GetMapping("/group/{groupId}/user/{userId}")
    public ResponseEntity<List<Expense>> getExpensesByUserInGroup(@PathVariable Long groupId, @PathVariable Long userId) {
        List<Expense> expenses = expenseService.getExpensesByUserInGroup(groupId, userId);
        return ResponseEntity.ok(expenses);
    }
}
