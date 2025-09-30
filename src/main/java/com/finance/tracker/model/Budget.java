package com.finance.tracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Budget entity representing a spending limit for a category
 * 
 * Each budget is unique per user, category, month, and year combination
 * Allows users to set spending limits and track against them
 */
@Entity
@Table(name = "budgets", 
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_category_month_year", 
            columnNames = {"user_id", "category_id", "month", "year"}
        )
    },
    indexes = {
        @Index(name = "idx_user_month_year", columnList = "user_id, month, year")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Budget {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @Column(name = "limit_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal limitAmount;
    
    @Column(nullable = false)
    private Integer month; // 1-12
    
    @Column(nullable = false)
    private Integer year;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
