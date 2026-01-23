package com.demo.backend.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response DTO for membership data
 * Used for API responses to show user-organization relationships
 */
@Getter
@Builder
public class MembershipResponse {
    
    private Long id;
    private String clerkMembershipId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * User information
     */
    private UserInfo user;
    
    /**
     * Organization information
     */
    private OrganizationInfo organization;
    
    /**
     * Role information
     */
    private RoleInfo role;
    
    @Getter
    @Builder
    public static class UserInfo {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String imageUrl;
    }
    
    @Getter
    @Builder
    public static class OrganizationInfo {
        private Long id;
        private String clerkOrgId;
        private String name;
        private String slug;
        private String imageUrl;
    }
    
    @Getter
    @Builder
    public static class RoleInfo {
        private Long id;
        private String name;
        private String description;
    }
}
