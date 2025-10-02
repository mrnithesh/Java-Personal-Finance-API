package com.finance.tracker.service;

import com.finance.tracker.dto.TransactionRequest;
import com.finance.tracker.dto.TransactionResponse;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.exception.UnauthorizedAccessException;
import com.finance.tracker.model.Category;
import com.finance.tracker.model.Transaction;
import com.finance.tracker.model.User;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing transactions
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Create a new transaction
     */
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, User currentUser) {
        
        // Validate category exists and belongs to user or is default
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        
        // Check if category belongs to user or is a default category
        if (category.getUser() != null && !category.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You can only use your own categories or default categories");
        }
        
        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setUser(currentUser);
        transaction.setCategory(category);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setTransactionType(request.getTransactionType());
        
        Transaction saved = transactionRepository.save(transaction);
        return mapToResponse(saved);
    }

    /**
     * Update an existing transaction
     */
    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request, User currentUser) {
        
        // Find transaction
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        
        // Verify ownership
        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You can only update your own transactions");
        }
        
        // Validate category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        
        if (category.getUser() != null && !category.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You can only use your own categories or default categories");
        }
        
        // Update fields
        transaction.setCategory(category);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setTransactionType(request.getTransactionType());
        
        Transaction updated = transactionRepository.save(transaction);
        return mapToResponse(updated);
    }

    /**
     * Delete a transaction
     */
    @Transactional
    public void deleteTransaction(Long id, User currentUser) {
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        
        // Verify ownership
        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You can only delete your own transactions");
        }
        
        transactionRepository.delete(transaction);
    }

    /**
     * Get a single transaction by ID
     */
    public TransactionResponse getTransactionById(Long id, User currentUser) {
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        
        // Verify ownership
        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You can only view your own transactions");
        }
        
        return mapToResponse(transaction);
    }

    /**
     * Get all transactions for a user with optional filters
     */
    public List<TransactionResponse> getAllTransactions(
            User currentUser, 
            LocalDate startDate, 
            LocalDate endDate, 
            Long categoryId) {
        
        List<Transaction> transactions;
        
        if (startDate != null && endDate != null && categoryId != null) {
            // Filter by date range and category
            transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                currentUser.getId(), startDate, endDate)
                .stream()
                .filter(t -> t.getCategory().getId().equals(categoryId))
                .collect(Collectors.toList());
        } else if (startDate != null && endDate != null) {
            // Filter by date range only
            transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                currentUser.getId(), startDate, endDate);
        } else if (categoryId != null) {
            // Filter by category only
            transactions = transactionRepository.findByUserIdAndCategoryId(
                currentUser.getId(), categoryId);
        } else {
            // No filters - get all transactions
            transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(
                currentUser.getId());
        }
        
        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map Transaction entity to TransactionResponse DTO
     */
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .categoryId(transaction.getCategory().getId())
                .categoryName(transaction.getCategory().getName())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .paymentMethod(transaction.getPaymentMethod())
                .transactionType(transaction.getTransactionType())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}

