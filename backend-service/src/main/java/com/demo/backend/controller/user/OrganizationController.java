package com.demo.backend.controller.user;

import com.demo.backend.dto.user.MembershipResponse;
import com.demo.backend.dto.user.OrganizationMembersResponse;
import com.demo.backend.dto.user.OrganizationResponse;
import com.demo.backend.service.user.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for organization endpoints
 * 
 * IMPORTANT: This controller trusts the API Gateway for authentication.
 * The gateway validates JWT and adds user context headers (X-User-Id, X-Org-Id).
 * This service does NOT parse JWT - it trusts the gateway.
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Slf4j
public class OrganizationController {
    
    private final OrganizationService organizationService;
    
    /**
     * Get all organizations for the current user
     * Returns organizations where user has membership
     * 
     * Example: GET /api/organizations
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserOrganizations(
            @RequestHeader(value = "X-User-Id", required = false) String clerkUserId) {
        
        log.info("GET /api/organizations called with X-User-Id: {}", clerkUserId);
        
        if (clerkUserId == null || clerkUserId.isEmpty()) {
            log.warn("Missing X-User-Id header");
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Missing user context"));
        }
        
        try {
            List<OrganizationResponse> organizations = organizationService.getUserOrganizations(clerkUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("organizations", organizations);
            response.put("total", organizations.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching organizations for user {}: {}", clerkUserId, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get organization by ID
     * Requires: User must have access to the organization
     * 
     * Example: GET /api/organizations/1
     */
    @GetMapping("/{orgId}")
    public ResponseEntity<OrganizationResponse> getOrganizationById(
            @PathVariable Long orgId,
            @RequestHeader(value = "X-User-Id", required = false) String clerkUserId) {
        
        log.info("GET /api/organizations/{} called with X-User-Id: {}", orgId, clerkUserId);
        
        if (clerkUserId == null || clerkUserId.isEmpty()) {
            log.warn("Missing X-User-Id header");
            return ResponseEntity.badRequest().build();
        }
        
        try {
            OrganizationResponse organization = organizationService.getOrganizationById(orgId, clerkUserId);
            return ResponseEntity.ok(organization);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            log.warn("Access denied for user {} to organization {}: {}", clerkUserId, orgId, e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (RuntimeException e) {
            log.error("Error fetching organization {}: {}", orgId, e.getMessage(), e);
            return ResponseEntity.status(404).build();
        }
    }
    
    /**
     * Get organization by Clerk org ID
     * Requires: User must have access to the organization
     * 
     * Example: GET /api/organizations/clerk/org_xxx
     */
    @GetMapping("/clerk/{clerkOrgId}")
    public ResponseEntity<OrganizationResponse> getOrganizationByClerkId(
            @PathVariable String clerkOrgId,
            @RequestHeader(value = "X-User-Id", required = false) String clerkUserId) {
        
        log.info("GET /api/organizations/clerk/{} called with X-User-Id: {}", clerkOrgId, clerkUserId);
        
        if (clerkUserId == null || clerkUserId.isEmpty()) {
            log.warn("Missing X-User-Id header");
            return ResponseEntity.badRequest().build();
        }
        
        try {
            OrganizationResponse organization = organizationService.getOrganizationByClerkId(clerkOrgId, clerkUserId);
            return ResponseEntity.ok(organization);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            log.warn("Access denied for user {} to organization {}: {}", clerkUserId, clerkOrgId, e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (RuntimeException e) {
            log.error("Error fetching organization {}: {}", clerkOrgId, e.getMessage(), e);
            return ResponseEntity.status(404).build();
        }
    }
    
    /**
     * Get all members of an organization
     * Requires: User must have access to the organization
     * 
     * Query parameters:
     * - page: Page number (0-indexed, default: 0)
     * - size: Page size (default: 20)
     * 
     * Example: GET /api/organizations/1/members?page=0&size=20
     */
    @GetMapping("/{orgId}/members")
    public ResponseEntity<OrganizationMembersResponse> getOrganizationMembers(
            @PathVariable Long orgId,
            @RequestHeader(value = "X-User-Id", required = false) String clerkUserId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        log.info("GET /api/organizations/{}/members called with X-User-Id: {}, page: {}, size: {}", 
            orgId, clerkUserId, page, size);
        
        if (clerkUserId == null || clerkUserId.isEmpty()) {
            log.warn("Missing X-User-Id header");
            return ResponseEntity.badRequest().build();
        }
        
        try {
            OrganizationMembersResponse response = organizationService.getOrganizationMembers(
                orgId, clerkUserId, page, size);
            return ResponseEntity.ok(response);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            log.warn("Access denied for user {} to organization {} members: {}", clerkUserId, orgId, e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (RuntimeException e) {
            log.error("Error fetching members for organization {}: {}", orgId, e.getMessage(), e);
            return ResponseEntity.status(404).build();
        }
    }
    
    /**
     * Get current user's memberships across all organizations
     * 
     * Example: GET /api/organizations/memberships
     */
    @GetMapping("/memberships")
    public ResponseEntity<Map<String, Object>> getUserMemberships(
            @RequestHeader(value = "X-User-Id", required = false) String clerkUserId) {
        
        log.info("GET /api/organizations/memberships called with X-User-Id: {}", clerkUserId);
        
        if (clerkUserId == null || clerkUserId.isEmpty()) {
            log.warn("Missing X-User-Id header");
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Missing user context"));
        }
        
        try {
            List<MembershipResponse> memberships = organizationService.getUserMemberships(clerkUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("memberships", memberships);
            response.put("total", memberships.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching memberships for user {}: {}", clerkUserId, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
}
