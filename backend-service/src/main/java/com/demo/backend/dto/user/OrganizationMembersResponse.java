package com.demo.backend.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Response DTO for organization members list
 * Includes pagination metadata
 */
@Getter
@Builder
public class OrganizationMembersResponse {
    
    /**
     * List of members in the organization
     */
    private List<MemberInfo> members;
    
    /**
     * Total number of members
     */
    private Long totalMembers;
    
    /**
     * Current page number (0-indexed)
     */
    private Integer page;
    
    /**
     * Page size
     */
    private Integer size;
    
    /**
     * Total number of pages
     */
    private Integer totalPages;
    
    @Getter
    @Builder
    public static class MemberInfo {
        private Long membershipId;
        private String clerkMembershipId;
        private String roleName;
        private Long roleId;
        private Long userId;
        private String email;
        private String firstName;
        private String lastName;
        private String imageUrl;
    }
}
