package com.finance.tracker.repository;

import com.finance.tracker.model.Category;
import com.finance.tracker.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Category entity
 * 
 * Provides CRUD operations and custom query methods for Category
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * Find all categories for a specific user
     * 
     * @param userId the user ID
     * @return list of user's categories
     */
    List<Category> findByUserId(Long userId);
    
    /**
     * Find all default (system-provided) categories
     * 
     * @return list of default categories
     */
    List<Category> findByIsDefaultTrue();
    
    /**
     * Find categories by user and type
     * 
     * @param userId the user ID
     * @param type the transaction type (INCOME or EXPENSE)
     * @return list of categories matching criteria
     */
    List<Category> findByUserIdAndType(Long userId, TransactionType type);
    
    /**
     * Find default categories by type
     * 
     * @param type the transaction type
     * @return list of default categories of specified type
     */
    List<Category> findByIsDefaultTrueAndType(TransactionType type);
}
