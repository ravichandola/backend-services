package com.demo.backend.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response DTO for user data
 * Excludes sensitive fields like clerkUserId (internal identifier)
 * Used for API responses to control what data is exposed
 */
@Getter
@Builder
public class UserResponse {
    
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String imageUrl;
    private LocalDateTime createdAt;
}
