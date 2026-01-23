package com.demo.backend.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response DTO for organization data
 * Used for API responses to control what data is exposed
 */
@Getter
@Builder
public class OrganizationResponse {
    
    private Long id;
    private String clerkOrgId;
    private String name;
    private String slug;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Total number of members in the organization
     */
    private Long memberCount;
    
    /**
     * Current user's role in this organization (if applicable)
     */
    private String userRole;
}
