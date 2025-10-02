package com.finance.tracker.controller;

import com.finance.tracker.dto.TransactionRequest;
import com.finance.tracker.dto.TransactionResponse;
import com.finance.tracker.model.User;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for transaction operations
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    /**
     * Create a new transaction
     * POST /api/transactions
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        TransactionResponse response = transactionService.createTransaction(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all transactions with optional filters
     * GET /api/transactions?startDate=2025-01-01&endDate=2025-12-31&categoryId=5
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        List<TransactionResponse> transactions = transactionService.getAllTransactions(
            currentUser, startDate, endDate, categoryId
        );
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get a single transaction by ID
     * GET /api/transactions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable Long id,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        TransactionResponse response = transactionService.getTransactionById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a transaction
     * PUT /api/transactions/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        TransactionResponse response = transactionService.updateTransaction(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a transaction
     * DELETE /api/transactions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long id,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        transactionService.deleteTransaction(id, currentUser);
        return ResponseEntity.noContent().build();
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

