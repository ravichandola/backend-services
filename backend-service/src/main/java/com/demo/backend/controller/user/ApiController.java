package com.demo.backend.controller.user;

import com.demo.backend.dto.user.UserResponse;
import com.demo.backend.entity.user.User;
import com.demo.backend.repository.user.MembershipRepository;
import com.demo.backend.repository.user.OrganizationRepository;
import com.demo.backend.repository.user.RoleRepository;
import com.demo.backend.repository.user.UserRepository;
import com.demo.backend.service.user.AuthorizationService;
import com.demo.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final UserService userService;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    
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
     * Get all users with their roles and memberships
     * Requires: User must be ADMIN in at least one organization
     * Gateway adds X-User-Id header
     * 
     * Query parameters:
     * - page: Page number (0-indexed, default: 0)
     * - size: Page size (default: 20)
     * - sort: Sort field and direction (e.g., "createdAt,desc", default: "createdAt,desc")
     * - includeRoles: true to include roles/memberships (default: false for backward compatibility)
     * 
     * Example: GET /api/users?page=0&size=20&includeRoles=true
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestHeader(value = "X-User-Id", required = false) String clerkUserId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,
            @RequestParam(value = "includeRoles", defaultValue = "false") boolean includeRoles) {
        
        log.info("GET /api/users called - user: {}, page: {}, size: {}, sort: {}, includeRoles: {}", 
            clerkUserId, page, size, sort, includeRoles);
        
        if (clerkUserId == null || clerkUserId.isEmpty()) {
            log.warn("Missing X-User-Id header");
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Missing user context"));
        }
        
        try {
            // Parse sort parameter (format: "field,direction")
            String[] sortParts = sort.split(",");
            String sortField = sortParts[0];
            org.springframework.data.domain.Sort.Direction direction = 
                sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                    ? org.springframework.data.domain.Sort.Direction.ASC
                    : org.springframework.data.domain.Sort.Direction.DESC;
            
            // Create Pageable with pagination and sorting
            Pageable pageable = PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(direction, sortField));
            
            // Build response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            
            if (includeRoles) {
                // Fetch users with roles
                Page<com.demo.backend.dto.user.UserWithRolesResponse> usersPage = 
                    userService.getAllUsersWithRoles(clerkUserId, pageable);
                
                response.put("content", usersPage.getContent());
                response.put("page", usersPage.getNumber());
                response.put("size", usersPage.getSize());
                response.put("totalElements", usersPage.getTotalElements());
                response.put("totalPages", usersPage.getTotalPages());
                response.put("first", usersPage.isFirst());
                response.put("last", usersPage.isLast());
                response.put("numberOfElements", usersPage.getNumberOfElements());
                
                log.info("Returning {} users with roles (page {} of {})", 
                    usersPage.getNumberOfElements(), 
                    usersPage.getNumber() + 1, 
                    usersPage.getTotalPages());
            } else {
                // Fetch users without roles (backward compatibility)
                Page<UserResponse> usersPage = userService.getAllUsers(clerkUserId, pageable);
                
                response.put("content", usersPage.getContent());
                response.put("page", usersPage.getNumber());
                response.put("size", usersPage.getSize());
                response.put("totalElements", usersPage.getTotalElements());
                response.put("totalPages", usersPage.getTotalPages());
                response.put("first", usersPage.isFirst());
                response.put("last", usersPage.isLast());
                response.put("numberOfElements", usersPage.getNumberOfElements());
                
                log.info("Returning {} users (page {} of {})", 
                    usersPage.getNumberOfElements(), 
                    usersPage.getNumber() + 1, 
                    usersPage.getTotalPages());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (org.springframework.security.access.AccessDeniedException e) {
            log.warn("Access denied for user {}: {}", clerkUserId, e.getMessage());
            return ResponseEntity.status(403)
                .body(Map.of("error", e.getMessage()));
        } catch (org.springframework.dao.InvalidDataAccessResourceUsageException e) {
            // Handle PostgreSQL prepared statement conflicts
            log.error("Database error while fetching users for {}: {}", clerkUserId, e.getMessage(), e);
            String errorMessage = "Database connection error";
            if (e.getMessage() != null && e.getMessage().contains("prepared statement")) {
                errorMessage = "Database connection error: prepared statement conflict. Please retry.";
            }
            return ResponseEntity.status(500)
                .body(Map.of("error", errorMessage, "message", "Internal server error occurred"));
        } catch (Exception e) {
            log.error("Error fetching users for user {}: {}", clerkUserId, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Internal server error", "message", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
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
        long memberCount = membershipRepository.countByOrganizationId(orgId);
        response.put("memberCount", memberCount);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Diagnostic endpoint: Check user's memberships and roles
     * Useful for debugging authorization issues
     * WARNING: Only available in dev profile
     */
    @Profile("dev")
    @GetMapping("/debug/my-memberships")
    public ResponseEntity<Map<String, Object>> getMyMemberships(
            @RequestHeader(value = "X-User-Id", required = false) String clerkUserId) {
        
        if (clerkUserId == null || clerkUserId.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Missing user context"));
        }
        
        return authorizationService.getUserByClerkId(clerkUserId)
            .map(user -> {
                Map<String, Object> response = new HashMap<>();
                response.put("clerkUserId", clerkUserId);
                response.put("userId", user.getId());
                response.put("email", user.getEmail());
                
                // Get all memberships for this user
                var memberships = membershipRepository.findByUserId(user.getId());
                var membershipList = memberships.stream().map(m -> {
                    Map<String, Object> mem = new HashMap<>();
                    mem.put("membershipId", m.getId());
                    mem.put("organizationId", m.getOrganization().getId());
                    mem.put("organizationName", m.getOrganization().getName());
                    mem.put("clerkOrgId", m.getOrganization().getClerkOrgId());
                    mem.put("roleName", m.getRole().getName());
                    mem.put("roleId", m.getRole().getId());
                    mem.put("clerkMembershipId", m.getClerkMembershipId());
                    return mem;
                }).toList();
                
                response.put("memberships", membershipList);
                response.put("totalMemberships", memberships.size());
                
                // Check admin status
                boolean isAdmin = authorizationService.isAdminInAnyOrganization(clerkUserId);
                response.put("isAdminInAnyOrganization", isAdmin);
                
                if (isAdmin) {
                    var adminMemberships = membershipRepository.findAdminMembershipsByUserId(user.getId());
                    response.put("adminMemberships", adminMemberships.size());
                }
                
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User not found", "clerkUserId", clerkUserId)));
    }
    
    /**
     * List available organizations for the user
     * Useful for finding organization IDs before fixing roles
     * WARNING: Only available in dev profile
     */
    @Profile("dev")
    @GetMapping("/debug/organizations")
    public ResponseEntity<Map<String, Object>> getOrganizations(
            @RequestHeader(value = "X-User-Id", required = false) String clerkUserId) {
        
        if (clerkUserId == null || clerkUserId.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Missing user context"));
        }
        
        var orgs = organizationRepository.findAll();
        var orgList = orgs.stream().map(o -> {
            Map<String, Object> org = new HashMap<>();
            org.put("id", o.getId());
            org.put("name", o.getName());
            org.put("clerkOrgId", o.getClerkOrgId());
            org.put("slug", o.getSlug());
            return org;
        }).toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("organizations", orgList);
        response.put("total", orgs.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Fix role endpoint: Update user's role in an organization
     * WARNING: Only available in dev profile - should be removed or secured in production
     * 
     * Request body (either organizationId OR clerkOrgId):
     * {
     *   "organizationId": 1,  // Optional: use database ID
     *   "clerkOrgId": "org_xxx",  // Optional: use Clerk org ID
     *   "roleName": "ADMIN"  // or "USER"
     * }
     */
    @Profile("dev")
    @PostMapping("/debug/fix-role")
    public ResponseEntity<Map<String, Object>> fixRole(
            @RequestHeader(value = "X-User-Id", required = false) String clerkUserId,
            @RequestBody Map<String, Object> request) {
        
        if (clerkUserId == null || clerkUserId.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Missing user context"));
        }
        
        Object orgIdObj = request.get("organizationId");
        Object clerkOrgIdObj = request.get("clerkOrgId");
        Object roleNameObj = request.get("roleName");
        
        if (roleNameObj == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "roleName is required"));
        }
        
        if (orgIdObj == null && clerkOrgIdObj == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Either organizationId or clerkOrgId is required"));
        }
        
        Long organizationId;
        
        // Try to find organization by ID or Clerk org ID
        if (orgIdObj != null) {
            try {
                if (orgIdObj instanceof Number) {
                    organizationId = ((Number) orgIdObj).longValue();
                } else {
                    organizationId = Long.parseLong(orgIdObj.toString());
                }
                
                // Verify organization exists
                if (!organizationRepository.existsById(organizationId)) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Organization not found", "organizationId", organizationId, 
                            "hint", "Use GET /api/debug/organizations to list available organizations"));
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid organizationId format"));
            }
        } else {
            // Find by Clerk org ID
            String clerkOrgId = clerkOrgIdObj.toString();
            var orgOpt = organizationRepository.findByClerkOrgId(clerkOrgId);
            if (orgOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Organization not found", "clerkOrgId", clerkOrgId,
                        "hint", "Use GET /api/debug/organizations to list available organizations"));
            }
            organizationId = orgOpt.get().getId();
        }
        
        String roleName = roleNameObj.toString().toUpperCase();
        if (!roleName.equals("ADMIN") && !roleName.equals("USER")) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "roleName must be ADMIN or USER"));
        }
        
        // Verify role exists
        if (roleRepository.findByName(roleName).isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Role not found", "roleName", roleName));
        }
        
        // Update role
        boolean updated = authorizationService.updateUserRole(clerkUserId, organizationId, roleName);
        
        if (updated) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Role updated successfully");
            response.put("clerkUserId", clerkUserId);
            response.put("organizationId", organizationId);
            response.put("roleName", roleName);
            
            // Return updated membership info
            var membershipOpt = authorizationService.getMembership(clerkUserId, organizationId);
            if (membershipOpt.isPresent()) {
                var m = membershipOpt.get();
                response.put("membership", Map.of(
                    "id", m.getId(),
                    "roleName", m.getRole().getName(),
                    "organizationName", m.getOrganization().getName()
                ));
            }
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to update role. Membership may not exist."));
        }
    }
    
    /**
     * Update user role in an organization
     * Requires: Admin must be ADMIN in the target organization
     * 
     * Request body:
     * {
     *   "userId": 1,  // Database user ID (optional, can use clerkUserId)
     *   "clerkUserId": "user_xxx",  // Clerk user ID (optional, can use userId)
     *   "organizationId": 1,  // Database organization ID (optional, can use clerkOrgId)
     *   "clerkOrgId": "org_xxx",  // Clerk organization ID (optional, can use organizationId)
     *   "roleName": "ADMIN"  // or "USER"
     * }
     */
    @PutMapping("/users/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @RequestHeader(value = "X-User-Id", required = false) String adminClerkUserId,
            @RequestBody Map<String, Object> request) {
        
        if (adminClerkUserId == null || adminClerkUserId.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Missing user context"));
        }
        
        // Check admin is authorized
        if (!authorizationService.isAdminInAnyOrganization(adminClerkUserId)) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "Forbidden: ADMIN role required"));
        }
        
        Object userIdObj = request.get("userId");
        Object clerkUserIdObj = request.get("clerkUserId");
        Object orgIdObj = request.get("organizationId");
        Object clerkOrgIdObj = request.get("clerkOrgId");
        Object roleNameObj = request.get("roleName");
        
        if (roleNameObj == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "roleName is required"));
        }
        
        if (userIdObj == null && clerkUserIdObj == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Either userId or clerkUserId is required"));
        }
        
        if (orgIdObj == null && clerkOrgIdObj == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Either organizationId or clerkOrgId is required"));
        }
        
        // Find user
        String targetClerkUserId;
        if (clerkUserIdObj != null) {
            targetClerkUserId = clerkUserIdObj.toString();
        } else {
            Long userId = ((Number) userIdObj).longValue();
            var userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "User not found", "userId", userId));
            }
            targetClerkUserId = userOpt.get().getClerkUserId();
        }
        
        // Find organization
        Long organizationId;
        if (orgIdObj != null) {
            try {
                organizationId = ((Number) orgIdObj).longValue();
                if (!organizationRepository.existsById(organizationId)) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Organization not found", "organizationId", organizationId));
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid organizationId format"));
            }
        } else {
            String clerkOrgId = clerkOrgIdObj.toString();
            var orgOpt = organizationRepository.findByClerkOrgId(clerkOrgId);
            if (orgOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Organization not found", "clerkOrgId", clerkOrgId));
            }
            organizationId = orgOpt.get().getId();
        }
        
        String roleName = roleNameObj.toString().toUpperCase();
        if (!roleName.equals("ADMIN") && !roleName.equals("USER")) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "roleName must be ADMIN or USER"));
        }
        
        // Verify role exists
        if (roleRepository.findByName(roleName).isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Role not found", "roleName", roleName));
        }
        
        // Update role
        boolean updated = authorizationService.updateUserRole(targetClerkUserId, organizationId, roleName);
        
        if (updated) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User role updated successfully");
            response.put("clerkUserId", targetClerkUserId);
            response.put("organizationId", organizationId);
            response.put("roleName", roleName);
            
            // Return updated membership info
            var membershipOpt = authorizationService.getMembership(targetClerkUserId, organizationId);
            if (membershipOpt.isPresent()) {
                var m = membershipOpt.get();
                response.put("membership", Map.of(
                    "id", m.getId(),
                    "roleName", m.getRole().getName(),
                    "organizationName", m.getOrganization().getName()
                ));
            }
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to update user role"));
        }
    }
    
    /**
     * TEST ENDPOINT: Create organization for testing
     * WARNING: Only available in dev profile - should be removed in production
     * 
     * Request body:
     * {
     *   "clerkOrgId": "org_xxx",
     *   "name": "Organization Name",
     *   "slug": "org-slug" (optional)
     * }
     */
    @Profile("dev")
    @PostMapping("/test/organizations")
    public ResponseEntity<Map<String, Object>> createTestOrganization(
            @RequestBody Map<String, String> request) {
        
        log.warn("⚠️  Test endpoint /api/test/organizations called - this should be removed in production!");
        
        String clerkOrgId = request.get("clerkOrgId");
        String name = request.get("name");
        String slug = request.get("slug");
        
        if (clerkOrgId == null || name == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "clerkOrgId and name are required"));
        }
        
        // Check if organization already exists
        if (organizationRepository.existsByClerkOrgId(clerkOrgId)) {
            var existingOrg = organizationRepository.findByClerkOrgId(clerkOrgId);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Organization already exists", 
                    "organization", Map.of(
                        "id", existingOrg.get().getId(),
                        "clerkOrgId", existingOrg.get().getClerkOrgId(),
                        "name", existingOrg.get().getName()
                    )));
        }
        
        // Create organization
        var org = com.demo.backend.entity.user.Organization.builder()
            .clerkOrgId(clerkOrgId)
            .name(name)
            .slug(slug)
            .build();
        
        organizationRepository.save(org);
        
        log.info("Test organization created: {} ({})", name, clerkOrgId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Organization created successfully");
        response.put("organization", Map.of(
            "id", org.getId(),
            "clerkOrgId", org.getClerkOrgId(),
            "name", org.getName(),
            "slug", org.getSlug() != null ? org.getSlug() : ""
        ));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * TEST ENDPOINT: Create user for testing
     * WARNING: Only available in dev profile - should be removed in production
     */
    @Profile("dev")
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
