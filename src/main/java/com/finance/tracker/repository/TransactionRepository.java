package com.finance.tracker.repository;

import com.finance.tracker.model.Transaction;
import com.finance.tracker.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Transaction entity
 * 
 * Provides CRUD operations and custom query methods for Transaction
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Find all transactions for a user, ordered by date (newest first)
     * 
     * @param userId the user ID
     * @return list of transactions ordered by date descending
     */
    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);
    
    /**
     * Find transactions for a user within a date range
     * 
     * @param userId the user ID
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of transactions in the date range
     */
    List<Transaction> findByUserIdAndTransactionDateBetween(
        Long userId, 
        LocalDate startDate, 
        LocalDate endDate
    );
    
    /**
     * Find transactions for a user and specific category
     * 
     * @param userId the user ID
     * @param categoryId the category ID
     * @return list of transactions for the category
     */
    List<Transaction> findByUserIdAndCategoryId(Long userId, Long categoryId);
    
    /**
     * Find transactions by user, type, and date range
     * 
     * @param userId the user ID
     * @param type the transaction type (INCOME or EXPENSE)
     * @param startDate the start date
     * @param endDate the end date
     * @return list of transactions matching criteria
     */
    List<Transaction> findByUserIdAndTransactionTypeAndTransactionDateBetween(
        Long userId,
        TransactionType type,
        LocalDate startDate,
        LocalDate endDate
    );
    
    /**
     * Calculate total amount for a user, category, and date range
     * Used for budget tracking
     * 
     * @param userId the user ID
     * @param categoryId the category ID
     * @param startDate the start date
     * @param endDate the end date
     * @return total amount (or 0 if no transactions)
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.category.id = :categoryId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserAndCategoryAndDateRange(
        @Param("userId") Long userId,
        @Param("categoryId") Long categoryId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
