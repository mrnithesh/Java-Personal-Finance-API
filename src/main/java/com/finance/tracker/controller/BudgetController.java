package com.finance.tracker.controller;

import com.finance.tracker.dto.BudgetAlertResponse;
import com.finance.tracker.dto.BudgetRequest;
import com.finance.tracker.dto.BudgetResponse;
import com.finance.tracker.model.User;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for budget operations
 */
@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final UserRepository userRepository;

    /**
     * Create a new budget
     * POST /api/budgets
     */
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @Valid @RequestBody BudgetRequest request,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        BudgetResponse response = budgetService.createBudget(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get budgets for a specific month and year
     * GET /api/budgets?month=9&year=2025
     * If no params provided, defaults to current month/year
     */
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        
        // Default to current month/year if not provided
        if (month == null || year == null) {
            LocalDate now = LocalDate.now();
            month = now.getMonthValue();
            year = now.getYear();
        }
        
        List<BudgetResponse> budgets = budgetService.getBudgets(currentUser, month, year);
        return ResponseEntity.ok(budgets);
    }

    /**
     * Get a single budget by ID
     * GET /api/budgets/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudgetById(
            @PathVariable Long id,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        BudgetResponse response = budgetService.getBudgetById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a budget
     * PUT /api/budgets/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        BudgetResponse response = budgetService.updateBudget(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a budget
     * DELETE /api/budgets/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable Long id,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        budgetService.deleteBudget(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get budget alerts (budgets exceeding 80% or 100%)
     * GET /api/budgets/alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<BudgetAlertResponse>> getBudgetAlerts(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        List<BudgetAlertResponse> alerts = budgetService.getBudgetAlerts(currentUser);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get current authenticated user from SecurityContext
     */
    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
