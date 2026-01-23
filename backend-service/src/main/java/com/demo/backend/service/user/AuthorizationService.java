package com.demo.backend.service.user;

import com.demo.backend.entity.user.Membership;
import com.demo.backend.entity.user.Role;
import com.demo.backend.entity.user.User;
import com.demo.backend.repository.user.MembershipRepository;
import com.demo.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
     */
    public boolean isAdminInAnyOrganization(String clerkUserId) {
        Optional<User> userOpt = userRepository.findByClerkUserId(clerkUserId);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", clerkUserId);
            return false;
        }
        
        Long userId = userOpt.get().getId();
        log.debug("Checking admin status for user ID: {}", userId);
        
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
        log.info("🔍 AuthorizationService.getUserByClerkId called with: '{}' (length: {})", clerkUserId, clerkUserId != null ? clerkUserId.length() : 0);
        
        // Check if user exists using existsByClerkUserId first
        boolean exists = userRepository.existsByClerkUserId(clerkUserId);
        log.info("🔍 User exists check: {}", exists);
        
        // Try to find all users to debug
        long totalUsers = userRepository.count();
        log.info("🔍 Total users in database: {}", totalUsers);
        
        Optional<User> user = userRepository.findByClerkUserId(clerkUserId);
        if (user.isPresent()) {
            log.info("✅ User found: id={}, email={}, clerkUserId='{}'", user.get().getId(), user.get().getEmail(), user.get().getClerkUserId());
        } else {
            log.warn("❌ User not found in database for clerkUserId: '{}'", clerkUserId);
            // Try to find by listing all users
            userRepository.findAll().forEach(u -> {
                log.info("🔍 Found user in DB: id={}, clerkUserId='{}' (length: {}), email={}", 
                    u.getId(), u.getClerkUserId(), u.getClerkUserId() != null ? u.getClerkUserId().length() : 0, u.getEmail());
            });
        }
        return user;
    }
}
