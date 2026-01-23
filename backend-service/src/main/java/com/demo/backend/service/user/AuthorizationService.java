package com.demo.backend.service.user;

import com.demo.backend.entity.user.Membership;
import com.demo.backend.entity.user.Organization;
import com.demo.backend.entity.user.Role;
import com.demo.backend.entity.user.User;
import com.demo.backend.repository.user.MembershipRepository;
import com.demo.backend.repository.user.OrganizationRepository;
import com.demo.backend.repository.user.RoleRepository;
import com.demo.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for authorization checks
 * Validates user permissions based on organization membership and roles
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {
    
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    
    /**
     * Check if user has access to an organization
     */
    public boolean hasAccessToOrganization(String clerkUserId, Long organizationId) {
        Optional<User> userOpt = userRepository.findByClerkUserId(clerkUserId);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", clerkUserId);
            return false;
        }
        
        Optional<Membership> membershipOpt = membershipRepository
            .findByUserIdAndOrganizationId(userOpt.get().getId(), organizationId);
        
        return membershipOpt.isPresent();
    }
    
    /**
     * Check if user has a specific role in an organization
     */
    public boolean hasRole(String clerkUserId, Long organizationId, String roleName) {
        Optional<User> userOpt = userRepository.findByClerkUserId(clerkUserId);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", clerkUserId);
            return false;
        }
        
        Optional<Membership> membershipOpt = membershipRepository
            .findByUserIdAndOrganizationIdAndRoleName(
                userOpt.get().getId(), 
                organizationId, 
                roleName.toUpperCase()
            );
        
        return membershipOpt.isPresent();
    }
    
    /**
     * Check if user is ADMIN of an organization
     */
    public boolean isAdmin(String clerkUserId, Long organizationId) {
        return hasRole(clerkUserId, organizationId, "ADMIN");
    }
    
    /**
     * Check if user is ADMIN in ANY organization
     * Used for system-wide admin operations (e.g., fetching all users)
     * 
     * @Transactional ensures proper transaction boundaries and helps avoid
     * prepared statement conflicts with PostgreSQL connection pooling
     */
    @Transactional(readOnly = true)
    public boolean isAdminInAnyOrganization(String clerkUserId) {
        Optional<User> userOpt = userRepository.findByClerkUserId(clerkUserId);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", clerkUserId);
            return false;
        }
        
        Long userId = userOpt.get().getId();
        log.debug("Checking admin status for user ID: {}", userId);
        
        try {
            List<Membership> adminMemberships = membershipRepository
                .findAdminMembershipsByUserId(userId);
            
            log.debug("Found {} admin memberships for user ID: {}", adminMemberships.size(), userId);
            
            boolean isAdmin = !adminMemberships.isEmpty();
            if (isAdmin) {
                log.info("User {} is ADMIN in {} organization(s)", clerkUserId, adminMemberships.size());
            } else {
                log.warn("User {} (ID: {}) is NOT admin - no ADMIN memberships found", clerkUserId, userId);
            }
            
            return isAdmin;
        } catch (org.springframework.dao.InvalidDataAccessResourceUsageException e) {
            // Handle PostgreSQL prepared statement conflicts
            if (e.getMessage() != null && e.getMessage().contains("prepared statement")) {
                log.warn("Prepared statement conflict detected, retrying with fresh transaction for user: {}", clerkUserId);
                // Retry in a new transaction to avoid "transaction aborted" error
                return retryAdminCheckInNewTransaction(userId, clerkUserId);
            }
            throw e;
        }
    }
    
    /**
     * Get user's membership in an organization
     */
    public Optional<Membership> getMembership(String clerkUserId, Long organizationId) {
        Optional<User> userOpt = userRepository.findByClerkUserId(clerkUserId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        
        return membershipRepository.findByUserIdAndOrganizationId(
            userOpt.get().getId(), 
            organizationId
        );
    }
    
    /**
     * Get user entity by Clerk user ID
     */
    public Optional<User> getUserByClerkId(String clerkUserId) {
        log.debug("Getting user by Clerk ID: {}", clerkUserId);
        
        Optional<User> user = userRepository.findByClerkUserId(clerkUserId);
        if (user.isPresent()) {
            log.debug("User found: id={}, email={}", user.get().getId(), user.get().getEmail());
        } else {
            log.warn("User not found for clerkUserId: {}", clerkUserId);
        }
        return user;
    }
    
    /**
     * Retry admin check in a new transaction
     * Used when prepared statement conflicts occur
     * Requires new transaction to avoid "transaction aborted" errors
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    private boolean retryAdminCheckInNewTransaction(Long userId, String clerkUserId) {
        try {
            long adminCount = membershipRepository.countAdminMembershipsByUserId(userId);
            boolean isAdmin = adminCount > 0;
            log.info("Retry successful - User {} is {} admin", clerkUserId, isAdmin ? "" : "NOT");
            return isAdmin;
        } catch (Exception retryException) {
            log.error("Retry also failed for user {}: {}", clerkUserId, retryException.getMessage());
            return false;
        }
    }
    
    /**
     * Update or create user's role in an organization
     * Used for fixing role assignments (e.g., when webhook didn't set role correctly)
     * Creates membership if it doesn't exist
     * 
     * @param clerkUserId The Clerk user ID
     * @param organizationId The organization ID
     * @param roleName The new role name (ADMIN or USER)
     * @return true if role was updated/created, false if user/org/role not found
     */
    @Transactional
    public boolean updateUserRole(String clerkUserId, Long organizationId, String roleName) {
        Optional<User> userOpt = userRepository.findByClerkUserId(clerkUserId);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", clerkUserId);
            return false;
        }
        
        User user = userOpt.get();
        Optional<Organization> orgOpt = organizationRepository.findById(organizationId);
        if (orgOpt.isEmpty()) {
            log.warn("Organization not found: {}", organizationId);
            return false;
        }
        
        Optional<Role> roleOpt = roleRepository.findByName(roleName.toUpperCase());
        if (roleOpt.isEmpty()) {
            log.warn("Role not found: {}", roleName);
            return false;
        }
        
        Optional<Membership> membershipOpt = membershipRepository
            .findByUserIdAndOrganizationId(user.getId(), organizationId);
        
        if (membershipOpt.isPresent()) {
            // Update existing membership
            Membership membership = membershipOpt.get();
            Role oldRole = membership.getRole();
            membership.setRole(roleOpt.get());
            membershipRepository.save(membership);
            
            log.info("Updated role for user {} in organization {} from {} to {}", 
                clerkUserId, organizationId, oldRole.getName(), roleName.toUpperCase());
        } else {
            // Create new membership
            String clerkMembershipId = "mem_fix_" + user.getId() + "_" + organizationId + "_" + System.currentTimeMillis();
            Membership membership = Membership.builder()
                .user(user)
                .organization(orgOpt.get())
                .role(roleOpt.get())
                .clerkMembershipId(clerkMembershipId)
                .build();
            
            membershipRepository.save(membership);
            
            log.info("Created membership for user {} in organization {} with role {}", 
                clerkUserId, organizationId, roleName.toUpperCase());
        }
        
        return true;
    }
}
