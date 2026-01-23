package com.demo.backend.service.user;

import com.demo.backend.dto.user.UserResponse;
import com.demo.backend.dto.user.UserWithRolesResponse;
import com.demo.backend.entity.user.Membership;
import com.demo.backend.entity.user.User;
import com.demo.backend.repository.user.MembershipRepository;
import com.demo.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for user-related business logic
 * Handles user data retrieval with proper authorization checks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final MembershipRepository membershipRepository;
    
    /**
     * Get all users with pagination
     * Requires: User must be ADMIN in at least one organization
     * 
     * @Transactional ensures proper transaction boundaries and prevents
     * transaction rollback issues
     * 
     * @param clerkUserId The Clerk user ID from gateway header (X-User-Id)
     * @param pageable Pagination parameters (page, size, sort)
     * @return Paginated list of users
     * @throws org.springframework.security.access.AccessDeniedException if user is not admin
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String clerkUserId, Pageable pageable) {
        log.info("Getting all users - requested by user: {}", clerkUserId);
        
        // Check authorization: User must be ADMIN in at least one organization
        if (!authorizationService.isAdminInAnyOrganization(clerkUserId)) {
            log.warn("User {} attempted to fetch all users without ADMIN role", clerkUserId);
            throw new org.springframework.security.access.AccessDeniedException(
                "Forbidden: ADMIN role required to fetch all users"
            );
        }
        
        // Apply default sorting if not specified (newest first)
        Pageable sortedPageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            sortedPageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }
        
        // Fetch users from repository with pagination
        Page<User> usersPage = userRepository.findAll(sortedPageable);
        
        log.info("Found {} users (page {} of {})", 
            usersPage.getTotalElements(), 
            usersPage.getNumber() + 1, 
            usersPage.getTotalPages());
        
        // Map entities to DTOs
        Page<UserResponse> responsePage = usersPage.map(this::mapToResponse);
        
        return responsePage;
    }
    
    /**
     * Get all users with their roles and memberships
     * Requires: User must be ADMIN in at least one organization
     * 
     * @Transactional ensures proper transaction boundaries for lazy loading
     * and prevents transaction rollback issues when mapping entities to DTOs
     * 
     * @param clerkUserId The Clerk user ID from gateway header (X-User-Id)
     * @param pageable Pagination parameters (page, size, sort)
     * @return Paginated list of users with roles
     * @throws org.springframework.security.access.AccessDeniedException if user is not admin
     */
    @Transactional(readOnly = true)
    public Page<UserWithRolesResponse> getAllUsersWithRoles(String clerkUserId, Pageable pageable) {
        log.info("Getting all users with roles - requested by user: {}", clerkUserId);
        
        // Check authorization: User must be ADMIN in at least one organization
        if (!authorizationService.isAdminInAnyOrganization(clerkUserId)) {
            log.warn("User {} attempted to fetch all users without ADMIN role", clerkUserId);
            throw new org.springframework.security.access.AccessDeniedException(
                "Forbidden: ADMIN role required to fetch all users"
            );
        }
        
        // Apply default sorting if not specified (newest first)
        Pageable sortedPageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            sortedPageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }
        
        // Fetch users from repository with pagination
        Page<User> usersPage = userRepository.findAll(sortedPageable);
        
        log.info("Found {} users (page {} of {})", 
            usersPage.getTotalElements(), 
            usersPage.getNumber() + 1, 
            usersPage.getTotalPages());
        
        // Map entities to DTOs with roles
        Page<UserWithRolesResponse> responsePage = usersPage.map(this::mapToUserWithRolesResponse);
        
        return responsePage;
    }
    
    /**
     * Map User entity to UserResponse DTO
     * Excludes sensitive fields like clerkUserId
     */
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .imageUrl(user.getImageUrl())
            .createdAt(user.getCreatedAt())
            .build();
    }
    
    /**
     * Map User entity to UserWithRolesResponse DTO
     * Includes memberships and role information
     */
    private UserWithRolesResponse mapToUserWithRolesResponse(User user) {
        try {
            // Get all memberships for this user with relationships eagerly fetched
            List<Membership> memberships = membershipRepository.findByUserIdWithRelations(user.getId());
            
            // Map memberships to DTO (safely handle lazy loading)
            List<UserWithRolesResponse.MembershipInfo> membershipInfos = memberships.stream()
                .map(m -> {
                    try {
                        // Access relationships within transaction context
                        return UserWithRolesResponse.MembershipInfo.builder()
                            .membershipId(m.getId())
                            .organizationId(m.getOrganization() != null ? m.getOrganization().getId() : null)
                            .organizationName(m.getOrganization() != null ? m.getOrganization().getName() : null)
                            .clerkOrgId(m.getOrganization() != null ? m.getOrganization().getClerkOrgId() : null)
                            .roleName(m.getRole() != null ? m.getRole().getName() : null)
                            .roleId(m.getRole() != null ? m.getRole().getId() : null)
                            .build();
                    } catch (Exception e) {
                        log.warn("Error mapping membership {}: {}", m.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(m -> m != null)
                .collect(Collectors.toList());
            
            // Check if user is admin in any organization
            boolean isAdmin = memberships.stream()
                .anyMatch(m -> m.getRole() != null && "ADMIN".equals(m.getRole().getName()));
            
            return UserWithRolesResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .createdAt(user.getCreatedAt())
                .memberships(membershipInfos)
                .totalOrganizations(memberships.size())
                .isAdmin(isAdmin)
                .build();
        } catch (Exception e) {
            log.error("Error mapping user {} to UserWithRolesResponse: {}", user.getId(), e.getMessage(), e);
            // Return user without memberships if error occurs
            return UserWithRolesResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .createdAt(user.getCreatedAt())
                .memberships(List.of())
                .totalOrganizations(0)
                .isAdmin(false)
                .build();
        }
    }
}
