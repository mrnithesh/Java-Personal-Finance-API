package com.finance.tracker.exception;

/**
 * Exception thrown when attempting to create a duplicate resource
 */
public class DuplicateResourceException extends RuntimeException {
    
    public DuplicateResourceException(String message) {
        super(message);
    }
}

