package com.demo.backend.dto.common;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Standard error response DTO for consistent error handling
 * Used across all controllers for uniform error responses
 */
@Getter
@Builder
public class ErrorResponse {
    
    /**
     * Error code for client-side error handling
     * Examples: "VALIDATION_ERROR", "AUTHORIZATION_ERROR", "NOT_FOUND", etc.
     */
    private String error;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * Timestamp when the error occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * Optional: Additional error details or hints
     */
    private String hint;
}
