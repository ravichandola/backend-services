package com.demo.backend.controller;

import com.demo.backend.entity.User;
import com.demo.backend.repository.MembershipRepository;
import com.demo.backend.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for protected endpoints
 * 
 * IMPORTANT: This controller trusts the API Gateway for authentication.
 * The gateway validates JWT and adds user context headers (X-User-Id, X-Org-Id).
 * This service does NOT parse JWT - it trusts the gateway.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {
    
    private final AuthorizationService authorizationService;
    private final MembershipRepository membershipRepository;
    
    /**
     * Health check endpoint
     * Accessible without authentication (or with authentication)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "backend-service");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current user information
     * Requires authentication (JWT validated by gateway)
     * Gateway adds X-User-Id header
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) String clerkUserId) {
        
        if (clerkUserId == null || clerkUserId.isEmpty()) {
            log.warn("Missing X-User-Id header");
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Missing user context"));
        }
        
        return authorizationService.getUserByClerkId(clerkUserId)
            .map(user -> {
                Map<String, Object> response = new HashMap<>();
                response.put("id", user.getId());
                response.put("clerkUserId", user.getClerkUserId());
                response.put("email", user.getEmail());
                response.put("firstName", user.getFirstName());
                response.put("lastName", user.getLastName());
                response.put("imageUrl", user.getImageUrl());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get admin data for an organization
     * Requires ADMIN role in the organization
     * Gateway adds X-User-Id and X-Org-Id headers
     */
    @GetMapping("/org/{orgId}/admin-data")
    public ResponseEntity<Map<String, Object>> getAdminData(
            @PathVariable Long orgId,
            @RequestHeader(value = "X-User-Id", required = false) String clerkUserId,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader) {
        
        if (clerkUserId == null || clerkUserId.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Missing user context"));
        }
        
        // Verify organization ID matches header (if provided)
        if (orgIdHeader != null && !orgIdHeader.equals(orgId.toString())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Organization ID mismatch"));
        }
        
        // Check if user is ADMIN of the organization
        if (!authorizationService.isAdmin(clerkUserId, orgId)) {
            log.warn("User {} attempted to access admin data for org {} without ADMIN role", 
                clerkUserId, orgId);
            return ResponseEntity.status(403)
                .body(Map.of("error", "Forbidden: ADMIN role required"));
        }
        
        // Return admin data
        Map<String, Object> response = new HashMap<>();
        response.put("organizationId", orgId);
        response.put("message", "This is admin-only data");
        response.put("timestamp", System.currentTimeMillis());
        
        // Get membership count (example admin data)
        long memberCount = membershipRepository.findByOrganizationId(orgId).size();
        response.put("memberCount", memberCount);
        
        return ResponseEntity.ok(response);
    }
}
