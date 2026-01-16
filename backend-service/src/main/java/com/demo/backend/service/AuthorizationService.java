package com.demo.backend.service;

import com.demo.backend.entity.Membership;
import com.demo.backend.entity.Role;
import com.demo.backend.entity.User;
import com.demo.backend.repository.MembershipRepository;
import com.demo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        return userRepository.findByClerkUserId(clerkUserId);
    }
}
