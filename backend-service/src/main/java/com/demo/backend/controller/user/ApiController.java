package com.demo.backend.controller.user;

import com.demo.backend.entity.user.User;
import com.demo.backend.repository.user.MembershipRepository;
import com.demo.backend.repository.user.UserRepository;
import com.demo.backend.service.user.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final UserRepository userRepository;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    // Constructor logging to verify bean creation
    {
        log.info("ApiController initialized - endpoint /api/me should be available");
    }
    
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
        
        log.info("GET /api/me called with X-User-Id: {}", clerkUserId);
        
        if (clerkUserId == null || clerkUserId.isEmpty()) {
            log.warn("Missing X-User-Id header");
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Missing user context"));
        }
        
        log.info("Looking up user with clerkUserId: {}", clerkUserId);
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
    
    /**
     * TEST ENDPOINT: Create user for testing
     * WARNING: This is for development/testing only - should be removed or secured in production
     */
    @PostMapping("/test/users")
    public ResponseEntity<Map<String, Object>> createTestUser(
            @RequestBody Map<String, String> request) {
        
        log.warn("⚠️  Test endpoint /api/test/users called - this should be removed in production!");
        
        String clerkUserId = request.get("clerkUserId");
        String email = request.get("email");
        String firstName = request.get("firstName");
        String lastName = request.get("lastName");
        
        if (clerkUserId == null || email == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "clerkUserId and email are required"));
        }
        
        // Check if user already exists
        if (userRepository.existsByClerkUserId(clerkUserId)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "User already exists", "clerkUserId", clerkUserId));
        }
        
        // Create user
        User user = User.builder()
            .clerkUserId(clerkUserId)
            .email(email)
            .firstName(firstName)
            .lastName(lastName)
            .build();
        
        userRepository.save(user);
        
        log.info("Test user created: {} ({})", email, clerkUserId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User created successfully");
        response.put("user", Map.of(
            "id", user.getId(),
            "clerkUserId", user.getClerkUserId(),
            "email", user.getEmail(),
            "firstName", user.getFirstName() != null ? user.getFirstName() : "",
            "lastName", user.getLastName() != null ? user.getLastName() : ""
        ));
        
        return ResponseEntity.ok(response);
    }
}
