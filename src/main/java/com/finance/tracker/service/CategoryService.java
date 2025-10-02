package com.finance.tracker.service;

import com.finance.tracker.dto.CategoryRequest;
import com.finance.tracker.dto.CategoryResponse;
import com.finance.tracker.exception.DuplicateResourceException;
import com.finance.tracker.model.Category;
import com.finance.tracker.model.TransactionType;
import com.finance.tracker.model.User;
import com.finance.tracker.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing categories
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Initialize default categories on application startup
     */
    @PostConstruct
    @Transactional
    public void initializeDefaultCategories() {
        
        // Check if default categories already exist
        List<Category> existingDefaults = categoryRepository.findByIsDefaultTrue();
        if (!existingDefaults.isEmpty()) {
            log.info("Default categories already initialized: {} categories found", existingDefaults.size());
            return;
        }
        
        log.info("Initializing default categories...");
        
        List<Category> defaultCategories = new ArrayList<>();
        
        // Income categories
        defaultCategories.add(createDefaultCategory("Salary", TransactionType.INCOME));
        defaultCategories.add(createDefaultCategory("Freelance", TransactionType.INCOME));
        defaultCategories.add(createDefaultCategory("Investment", TransactionType.INCOME));
        defaultCategories.add(createDefaultCategory("Gift", TransactionType.INCOME));
        defaultCategories.add(createDefaultCategory("Other Income", TransactionType.INCOME));
        
        // Expense categories
        defaultCategories.add(createDefaultCategory("Food & Dining", TransactionType.EXPENSE));
        defaultCategories.add(createDefaultCategory("Transportation", TransactionType.EXPENSE));
        defaultCategories.add(createDefaultCategory("Shopping", TransactionType.EXPENSE));
        defaultCategories.add(createDefaultCategory("Entertainment", TransactionType.EXPENSE));
        defaultCategories.add(createDefaultCategory("Bills & Utilities", TransactionType.EXPENSE));
        defaultCategories.add(createDefaultCategory("Healthcare", TransactionType.EXPENSE));
        defaultCategories.add(createDefaultCategory("Education", TransactionType.EXPENSE));
        defaultCategories.add(createDefaultCategory("Travel", TransactionType.EXPENSE));
        defaultCategories.add(createDefaultCategory("Groceries", TransactionType.EXPENSE));
        defaultCategories.add(createDefaultCategory("Rent", TransactionType.EXPENSE));
        defaultCategories.add(createDefaultCategory("Other Expense", TransactionType.EXPENSE));
        
        categoryRepository.saveAll(defaultCategories);
        
        log.info("Successfully initialized {} default categories", defaultCategories.size());
    }

    /**
     * Create a default category
     */
    private Category createDefaultCategory(String name, TransactionType type) {
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setIsDefault(true);
        category.setUser(null); // Default categories have no user
        return category;
    }

    /**
     * Create a custom user category
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, User currentUser) {
        
        // Check if user already has a category with this name
        List<Category> userCategories = categoryRepository.findByUserId(currentUser.getId());
        boolean exists = userCategories.stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(request.getName()));
        
        if (exists) {
            throw new DuplicateResourceException(
                "You already have a category named '" + request.getName() + "'"
            );
        }
        
        // Create category
        Category category = new Category();
        category.setName(request.getName());
        category.setType(request.getType());
        category.setIsDefault(false);
        category.setUser(currentUser);
        
        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    /**
     * Get all categories available to user (default + custom)
     */
    public List<CategoryResponse> getUserCategories(User currentUser) {
        
        // Get default categories
        List<Category> defaultCategories = categoryRepository.findByIsDefaultTrue();
        
        // Get user's custom categories
        List<Category> userCategories = categoryRepository.findByUserId(currentUser.getId());
        
        // Combine both lists
        List<Category> allCategories = new ArrayList<>();
        allCategories.addAll(defaultCategories);
        allCategories.addAll(userCategories);
        
        return allCategories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map Category entity to CategoryResponse DTO
     */
    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .isDefault(category.getIsDefault())
                .build();
    }
}

