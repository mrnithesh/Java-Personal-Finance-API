package com.finance.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for budget responses with spending information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetResponse {
    
    private Long id;
    private Long categoryId;
    private String categoryName;
    private BigDecimal limitAmount;
    private BigDecimal currentSpending;
    private Integer month;
    private Integer year;
    private Double percentageUsed;
    private Boolean isExceeded;
    private BigDecimal remaining;
    private LocalDateTime createdAt;
}
