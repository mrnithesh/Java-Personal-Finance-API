package com.finance.tracker.repository;

import com.finance.tracker.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Budget entity
 * 
 * Provides CRUD operations and custom query methods for Budget
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    /**
     * Find all budgets for a user in a specific month and year
     * 
     * @param userId the user ID
     * @param month the month (1-12)
     * @param year the year
     * @return list of budgets for the specified period
     */
    List<Budget> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);
    
    /**
     * Find a specific budget by user, category, month, and year
     * 
     * @param userId the user ID
     * @param categoryId the category ID
     * @param month the month (1-12)
     * @param year the year
     * @return Optional containing the budget if found
     */
    Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYear(
        Long userId, 
        Long categoryId, 
        Integer month, 
        Integer year
    );
    
    /**
     * Find all budgets for a user
     * 
     * @param userId the user ID
     * @return list of all budgets for the user
     */
    List<Budget> findByUserId(Long userId);
    
    /**
     * Find budgets for a specific category
     * 
     * @param categoryId the category ID
     * @return list of budgets for the category
     */
    List<Budget> findByCategoryId(Long categoryId);
}
