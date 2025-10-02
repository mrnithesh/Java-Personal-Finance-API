package com.finance.tracker.service;

import com.finance.tracker.dto.BudgetAlertResponse;
import com.finance.tracker.dto.BudgetRequest;
import com.finance.tracker.dto.BudgetResponse;
import com.finance.tracker.exception.DuplicateResourceException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.exception.UnauthorizedAccessException;
import com.finance.tracker.model.*;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing budgets
 */
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Create a new budget for a specific category and month.
     * Prevents duplicate budgets for the same category/month/year combination.
     */
    @Transactional
    public BudgetResponse createBudget(BudgetRequest request, User currentUser) {
        
        // First, verify the category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        
        // Users can create budgets for default categories or their own custom categories
        if (category.getUser() != null && !category.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You can only create budgets for your own categories or default categories");
        }
        
        // Prevent duplicate budgets - one budget per category per month
        Optional<Budget> existing = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
            currentUser.getId(), request.getCategoryId(), request.getMonth(), request.getYear()
        );
        
        if (existing.isPresent()) {
            throw new DuplicateResourceException(
                String.format("Budget already exists for category '%s' in %d/%d", 
                    category.getName(), request.getMonth(), request.getYear())
            );
        }
        
        // All validations passed - create the budget
        Budget budget = new Budget();
        budget.setUser(currentUser);
        budget.setCategory(category);
        budget.setLimitAmount(request.getLimitAmount());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());
        
        Budget saved = budgetRepository.save(budget);
        return mapToResponse(saved);
    }

    /**
     * Update an existing budget
     */
    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request, User currentUser) {
        
        // Find budget
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", "id", id));
        
        // Verify ownership
        if (!budget.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You can only update your own budgets");
        }
        
        // Validate category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        
        if (category.getUser() != null && !category.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You can only use your own categories or default categories");
        }
        
        // Check for duplicate if changing category/month/year
        if (!budget.getCategory().getId().equals(request.getCategoryId()) ||
            !budget.getMonth().equals(request.getMonth()) ||
            !budget.getYear().equals(request.getYear())) {
            
            Optional<Budget> existing = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                currentUser.getId(), request.getCategoryId(), request.getMonth(), request.getYear()
            );
            
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new DuplicateResourceException(
                    String.format("Budget already exists for category '%s' in %d/%d", 
                        category.getName(), request.getMonth(), request.getYear())
                );
            }
        }
        
        // Update fields
        budget.setCategory(category);
        budget.setLimitAmount(request.getLimitAmount());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());
        
        Budget updated = budgetRepository.save(budget);
        return mapToResponse(updated);
    }

    /**
     * Delete a budget
     */
    @Transactional
    public void deleteBudget(Long id, User currentUser) {
        
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", "id", id));
        
        // Verify ownership
        if (!budget.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You can only delete your own budgets");
        }
        
        budgetRepository.delete(budget);
    }

    /**
     * Get budgets for a specific month and year
     */
    public List<BudgetResponse> getBudgets(User currentUser, int month, int year) {
        
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(
            currentUser.getId(), month, year
        );
        
        return budgets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get budget by ID
     */
    public BudgetResponse getBudgetById(Long id, User currentUser) {
        
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", "id", id));
        
        // Verify ownership
        if (!budget.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You can only view your own budgets");
        }
        
        return mapToResponse(budget);
    }

    /**
     * Get budget alerts for current month.
     * Returns budgets that are at 80% (WARNING) or 100%+ (DANGER) of their limit.
     */
    public List<BudgetAlertResponse> getBudgetAlerts(User currentUser) {
        
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        
        // Fetch all budgets for the current month
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(
            currentUser.getId(), currentMonth, currentYear
        );
        
        List<BudgetAlertResponse> alerts = new ArrayList<>();
        
        // Check each budget against its spending
        for (Budget budget : budgets) {
            BigDecimal spending = calculateCurrentSpending(budget);
            double percentageUsed = calculatePercentage(spending, budget.getLimitAmount());
            
            // Alert thresholds: 80% = WARNING, 100% = DANGER
            if (percentageUsed >= 80.0) {
                int daysLeft = YearMonth.of(currentYear, currentMonth).lengthOfMonth() - now.getDayOfMonth();
                
                String alertLevel = percentageUsed >= 100.0 ? "DANGER" : "WARNING";
                String message = percentageUsed >= 100.0 
                    ? String.format("Budget exceeded by %.2f%%", percentageUsed - 100)
                    : String.format("%.0f%% of budget used with %d days remaining", percentageUsed, daysLeft);
                
                alerts.add(BudgetAlertResponse.builder()
                        .budgetId(budget.getId())
                        .categoryName(budget.getCategory().getName())
                        .limitAmount(budget.getLimitAmount())
                        .currentSpending(spending)
                        .percentageUsed(percentageUsed)
                        .daysLeftInMonth(daysLeft)
                        .alertLevel(alertLevel)
                        .message(message)
                        .build());
            }
        }
        
        return alerts;
    }

    /**
     * Calculate current spending for a budget by summing relevant transactions.
     * Note: We calculate on-demand rather than storing the value to ensure accuracy
     * and avoid synchronization issues when transactions are added/deleted.
     */
    private BigDecimal calculateCurrentSpending(Budget budget) {
        
        // Determine the date range for this budget's month
        LocalDate startDate = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        
        // Fetch all transactions in the date range
        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
            budget.getUser().getId(), startDate, endDate
        );
        
        // Sum only EXPENSE transactions that match this budget's category
        BigDecimal total = transactions.stream()
                .filter(t -> t.getCategory().getId().equals(budget.getCategory().getId()))
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return total;
    }

    /**
     * Calculate percentage used
     */
    private double calculatePercentage(BigDecimal spending, BigDecimal limit) {
        if (limit.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return spending.divide(limit, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    /**
     * Map Budget entity to BudgetResponse DTO
     */
    private BudgetResponse mapToResponse(Budget budget) {
        
        BigDecimal currentSpending = calculateCurrentSpending(budget);
        double percentageUsed = calculatePercentage(currentSpending, budget.getLimitAmount());
        BigDecimal remaining = budget.getLimitAmount().subtract(currentSpending);
        
        return BudgetResponse.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .limitAmount(budget.getLimitAmount())
                .currentSpending(currentSpending)
                .month(budget.getMonth())
                .year(budget.getYear())
                .percentageUsed(percentageUsed)
                .isExceeded(currentSpending.compareTo(budget.getLimitAmount()) > 0)
                .remaining(remaining)
                .createdAt(budget.getCreatedAt())
                .build();
    }
}
