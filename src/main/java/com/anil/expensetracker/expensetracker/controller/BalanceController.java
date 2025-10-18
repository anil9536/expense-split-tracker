package com.anil.expensetracker.expensetracker.controller;

import com.anil.expensetracker.expensetracker.dto.BalanceResponse;
import com.anil.expensetracker.expensetracker.dto.SettlementRequest;
import com.anil.expensetracker.expensetracker.model.Settlement;
import com.anil.expensetracker.expensetracker.service.BalanceService;
import com.anil.expensetracker.expensetracker.service.DebtSimplificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/balances")
@RequiredArgsConstructor
public class BalanceController {
    
    private final BalanceService balanceService;
    private final DebtSimplificationService debtSimplificationService;
    
    @GetMapping("/group/{groupId}/user/{userId}")
    public ResponseEntity<BalanceResponse> getUserBalanceInGroup(@PathVariable Long groupId, @PathVariable Long userId) {
        try {
            BalanceResponse balance = balanceService.getUserBalanceInGroup(groupId, userId);
            return ResponseEntity.ok(balance);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/settlements")
    public ResponseEntity<Settlement> createSettlement(@Valid @RequestBody SettlementRequest request) {
        try {
            Settlement settlement = balanceService.createSettlement(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(settlement);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/settlements/group/{groupId}")
    public ResponseEntity<List<Settlement>> getSettlementsByGroup(@PathVariable Long groupId) {
        List<Settlement> settlements = balanceService.getSettlementsByGroup(groupId);
        return ResponseEntity.ok(settlements);
    }
    
    @GetMapping("/settlements/group/{groupId}/user/{userId}")
    public ResponseEntity<List<Settlement>> getSettlementsByUserInGroup(@PathVariable Long groupId, @PathVariable Long userId) {
        List<Settlement> settlements = balanceService.getSettlementsByUserInGroup(groupId, userId);
        return ResponseEntity.ok(settlements);
    }
    
    @PostMapping("/simplify/group/{groupId}")
    public ResponseEntity<List<Settlement>> simplifyDebts(@PathVariable Long groupId) {
        try {
            List<Settlement> settlements = debtSimplificationService.simplifyDebts(groupId);
            return ResponseEntity.ok(settlements);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
