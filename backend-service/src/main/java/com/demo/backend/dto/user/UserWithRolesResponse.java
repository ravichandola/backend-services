package com.demo.backend.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for user data with roles and memberships
 * Used for admin user management
 */
@Getter
@Builder
public class UserWithRolesResponse {
    
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String imageUrl;
    private LocalDateTime createdAt;
    
    /**
     * List of memberships with organization and role info
     */
    private List<MembershipInfo> memberships;
    
    /**
     * Total number of organizations user belongs to
     */
    private Integer totalOrganizations;
    
    /**
     * Whether user is admin in any organization
     */
    private Boolean isAdmin;
    
    @Getter
    @Builder
    public static class MembershipInfo {
        private Long membershipId;
        private Long organizationId;
        private String organizationName;
        private String clerkOrgId;
        private String roleName;
        private Long roleId;
    }
}
