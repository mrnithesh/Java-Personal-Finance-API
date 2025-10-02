package com.finance.tracker.exception;

/**
 * Exception thrown when user tries to access resource they don't own
 */
public class UnauthorizedAccessException extends RuntimeException {
    
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}

