package com.finance.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for budget alert notifications
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetAlertResponse {
    
    private Long budgetId;
    private String categoryName;
    private BigDecimal limitAmount;
    private BigDecimal currentSpending;
    private Double percentageUsed;
    private Integer daysLeftInMonth;
    private String alertLevel;  // "WARNING" (80%), "DANGER" (100%)
    private String message;
}
