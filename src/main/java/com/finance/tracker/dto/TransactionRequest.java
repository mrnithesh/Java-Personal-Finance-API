package com.finance.tracker.dto;

import com.finance.tracker.model.PaymentMethod;
import com.finance.tracker.model.TransactionType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating/updating transactions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    
    @NotNull(message = "Category ID is required")
    private Long categoryId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 digits and 2 decimal places")
    private BigDecimal amount;
    
    @Size(max = 255, message = "Description must be less than 255 characters")
    private String description;
    
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;
    
    private PaymentMethod paymentMethod;
    
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;
}

