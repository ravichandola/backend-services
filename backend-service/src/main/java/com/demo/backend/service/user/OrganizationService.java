package com.demo.backend.service.user;

import com.demo.backend.dto.user.MembershipResponse;
import com.demo.backend.dto.user.OrganizationMembersResponse;
import com.demo.backend.dto.user.OrganizationResponse;
import com.demo.backend.entity.user.Membership;
import com.demo.backend.entity.user.Organization;
import com.demo.backend.entity.user.User;
import com.demo.backend.repository.user.MembershipRepository;
import com.demo.backend.repository.user.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for organization-related business logic
 * Handles organization data retrieval with proper authorization checks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {
    
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final AuthorizationService authorizationService;
    
    /**
     * Get all organizations for the current user
     * Returns organizations where user has membership
     * 
     * @param clerkUserId The Clerk user ID from gateway header (X-User-Id)
     * @return List of organizations user belongs to
     */
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getUserOrganizations(String clerkUserId) {
        log.info("Getting organizations for user: {}", clerkUserId);
        
        // Find user
        User user = authorizationService.getUserByClerkId(clerkUserId)
            .orElseThrow(() -> new RuntimeException("User not found: " + clerkUserId));
        
        // Get all memberships for this user with organizations
        List<Membership> memberships = membershipRepository.findByUserIdWithRelations(user.getId());
        
        // Map to response DTOs
        return memberships.stream()
            .map(membership -> {
                Organization org = membership.getOrganization();
                return OrganizationResponse.builder()
                    .id(org.getId())
                    .clerkOrgId(org.getClerkOrgId())
                    .name(org.getName())
                    .slug(org.getSlug())
                    .imageUrl(org.getImageUrl())
                    .createdAt(org.getCreatedAt())
                    .updatedAt(org.getUpdatedAt())
                    .memberCount(membershipRepository.countByOrganizationId(org.getId()))
                    .userRole(membership.getRole() != null ? membership.getRole().getName() : null)
                    .build();
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Get organization by ID
     * Requires: User must have access to the organization
     * 
     * @param orgId Organization database ID
     * @param clerkUserId The Clerk user ID from gateway header (X-User-Id)
     * @return Organization details
     */
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(Long orgId, String clerkUserId) {
        log.info("Getting organization {} for user: {}", orgId, clerkUserId);
        
        // Check authorization
        if (!authorizationService.hasAccessToOrganization(clerkUserId, orgId)) {
            log.warn("User {} attempted to access organization {} without access", clerkUserId, orgId);
            throw new org.springframework.security.access.AccessDeniedException(
                "Forbidden: User does not have access to this organization"
            );
        }
        
        // Find organization
        Organization org = organizationRepository.findById(orgId)
            .orElseThrow(() -> new RuntimeException("Organization not found: " + orgId));
        
        // Get user's membership to find their role
        User user = authorizationService.getUserByClerkId(clerkUserId)
            .orElseThrow(() -> new RuntimeException("User not found: " + clerkUserId));
        
        Optional<Membership> membershipOpt = membershipRepository
            .findByUserIdAndOrganizationId(user.getId(), orgId);
        
        String userRole = membershipOpt
            .map(m -> m.getRole() != null ? m.getRole().getName() : null)
            .orElse(null);
        
        return OrganizationResponse.builder()
            .id(org.getId())
            .clerkOrgId(org.getClerkOrgId())
            .name(org.getName())
            .slug(org.getSlug())
            .imageUrl(org.getImageUrl())
            .createdAt(org.getCreatedAt())
            .updatedAt(org.getUpdatedAt())
            .memberCount(membershipRepository.countByOrganizationId(orgId))
            .userRole(userRole)
            .build();
    }
    
    /**
     * Get organization by Clerk org ID
     * Requires: User must have access to the organization
     * 
     * @param clerkOrgId Clerk organization ID
     * @param clerkUserId The Clerk user ID from gateway header (X-User-Id)
     * @return Organization details
     */
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationByClerkId(String clerkOrgId, String clerkUserId) {
        log.info("Getting organization {} (Clerk ID) for user: {}", clerkOrgId, clerkUserId);
        
        // Find organization
        Organization org = organizationRepository.findByClerkOrgId(clerkOrgId)
            .orElseThrow(() -> new RuntimeException("Organization not found: " + clerkOrgId));
        
        // Check authorization
        if (!authorizationService.hasAccessToOrganization(clerkUserId, org.getId())) {
            log.warn("User {} attempted to access organization {} without access", clerkUserId, org.getId());
            throw new org.springframework.security.access.AccessDeniedException(
                "Forbidden: User does not have access to this organization"
            );
        }
        
        // Get user's membership to find their role
        User user = authorizationService.getUserByClerkId(clerkUserId)
            .orElseThrow(() -> new RuntimeException("User not found: " + clerkUserId));
        
        Optional<Membership> membershipOpt = membershipRepository
            .findByUserIdAndOrganizationId(user.getId(), org.getId());
        
        String userRole = membershipOpt
            .map(m -> m.getRole() != null ? m.getRole().getName() : null)
            .orElse(null);
        
        return OrganizationResponse.builder()
            .id(org.getId())
            .clerkOrgId(org.getClerkOrgId())
            .name(org.getName())
            .slug(org.getSlug())
            .imageUrl(org.getImageUrl())
            .createdAt(org.getCreatedAt())
            .updatedAt(org.getUpdatedAt())
            .memberCount(membershipRepository.countByOrganizationId(org.getId()))
            .userRole(userRole)
            .build();
    }
    
    /**
     * Get all members of an organization
     * Requires: User must have access to the organization
     * 
     * @param orgId Organization database ID
     * @param clerkUserId The Clerk user ID from gateway header (X-User-Id)
     * @param page Page number (0-indexed, default: 0)
     * @param size Page size (default: 20)
     * @return Paginated list of organization members
     */
    @Transactional(readOnly = true)
    public OrganizationMembersResponse getOrganizationMembers(
            Long orgId, 
            String clerkUserId, 
            int page, 
            int size) {
        log.info("Getting members for organization {} (requested by user: {})", orgId, clerkUserId);
        
        // Check authorization
        if (!authorizationService.hasAccessToOrganization(clerkUserId, orgId)) {
            log.warn("User {} attempted to access organization {} members without access", clerkUserId, orgId);
            throw new org.springframework.security.access.AccessDeniedException(
                "Forbidden: User does not have access to this organization"
            );
        }
        
        // Verify organization exists
        if (!organizationRepository.existsById(orgId)) {
            throw new RuntimeException("Organization not found: " + orgId);
        }
        
        // Get all memberships for organization with relations (we'll paginate manually since we don't have Pageable query)
        List<Membership> allMemberships = membershipRepository.findByOrganizationIdWithRelations(orgId);
        long totalCount = membershipRepository.countByOrganizationId(orgId);
        
        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, allMemberships.size());
        List<Membership> paginatedMemberships = start < allMemberships.size() 
            ? allMemberships.subList(start, end)
            : List.of();
        
        // Map to response DTOs
        List<OrganizationMembersResponse.MemberInfo> members = paginatedMemberships.stream()
            .map(membership -> {
                User user = membership.getUser();
                return OrganizationMembersResponse.MemberInfo.builder()
                    .membershipId(membership.getId())
                    .clerkMembershipId(membership.getClerkMembershipId())
                    .roleName(membership.getRole() != null ? membership.getRole().getName() : null)
                    .roleId(membership.getRole() != null ? membership.getRole().getId() : null)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .imageUrl(user.getImageUrl())
                    .build();
            })
            .collect(Collectors.toList());
        
        return OrganizationMembersResponse.builder()
            .members(members)
            .totalMembers(totalCount)
            .page(page)
            .size(size)
            .totalPages((int) Math.ceil((double) totalCount / size))
            .build();
    }
    
    /**
     * Get user's memberships across all organizations
     * 
     * @param clerkUserId The Clerk user ID from gateway header (X-User-Id)
     * @return List of user's memberships
     */
    @Transactional(readOnly = true)
    public List<MembershipResponse> getUserMemberships(String clerkUserId) {
        log.info("Getting memberships for user: {}", clerkUserId);
        
        // Find user
        User user = authorizationService.getUserByClerkId(clerkUserId)
            .orElseThrow(() -> new RuntimeException("User not found: " + clerkUserId));
        
        // Get all memberships for this user with relations
        List<Membership> memberships = membershipRepository.findByUserIdWithRelations(user.getId());
        
        // Map to response DTOs
        return memberships.stream()
            .map(membership -> {
                Organization org = membership.getOrganization();
                return MembershipResponse.builder()
                    .id(membership.getId())
                    .clerkMembershipId(membership.getClerkMembershipId())
                    .createdAt(membership.getCreatedAt())
                    .updatedAt(membership.getUpdatedAt())
                    .user(MembershipResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .imageUrl(user.getImageUrl())
                        .build())
                    .organization(MembershipResponse.OrganizationInfo.builder()
                        .id(org.getId())
                        .clerkOrgId(org.getClerkOrgId())
                        .name(org.getName())
                        .slug(org.getSlug())
                        .imageUrl(org.getImageUrl())
                        .build())
                    .role(MembershipResponse.RoleInfo.builder()
                        .id(membership.getRole().getId())
                        .name(membership.getRole().getName())
                        .description(membership.getRole().getDescription())
                        .build())
                    .build();
            })
            .collect(Collectors.toList());
    }
}
