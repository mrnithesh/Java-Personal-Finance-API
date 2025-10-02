package com.finance.tracker.dto;

import com.finance.tracker.model.PaymentMethod;
import com.finance.tracker.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for transaction responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    
    private Long id;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private String description;
    private LocalDate transactionDate;
    private PaymentMethod paymentMethod;
    private TransactionType transactionType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

