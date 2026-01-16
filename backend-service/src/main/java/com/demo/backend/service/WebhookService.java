package com.demo.backend.service;

import com.demo.backend.entity.*;
import com.demo.backend.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for processing Clerk webhook events
 * Handles user and organization lifecycle events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {
    
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final MembershipRepository membershipRepository;
    private final UserEventRepository userEventRepository;
    private final OrganizationEventRepository organizationEventRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public void processUserCreated(JsonNode eventData) {
        try {
            JsonNode data = eventData.get("data");
            String clerkUserId = data.get("id").asText();
            String email = data.has("email_addresses") && data.get("email_addresses").isArray() && data.get("email_addresses").size() > 0
                ? data.get("email_addresses").get(0).get("email_address").asText()
                : null;
            
            if (email == null) {
                log.warn("User created event missing email: {}", clerkUserId);
                return;
            }
            
            // Check if user already exists (idempotency)
            if (userRepository.existsByClerkUserId(clerkUserId)) {
                log.info("User already exists, skipping: {}", clerkUserId);
                return;
            }
            
            User user = User.builder()
                .clerkUserId(clerkUserId)
                .email(email)
                .firstName(data.has("first_name") ? data.get("first_name").asText() : null)
                .lastName(data.has("last_name") ? data.get("last_name").asText() : null)
                .imageUrl(data.has("image_url") ? data.get("image_url").asText() : null)
                .build();
            
            userRepository.save(user);
            log.info("User created: {} ({})", email, clerkUserId);
            
            // Store event for audit
            storeUserEvent(clerkUserId, "user.created", eventData);
            
        } catch (Exception e) {
            log.error("Error processing user.created event", e);
            throw new RuntimeException("Failed to process user.created event", e);
        }
    }
    
    @Transactional
    public void processUserUpdated(JsonNode eventData) {
        try {
            JsonNode data = eventData.get("data");
            String clerkUserId = data.get("id").asText();
            
            Optional<User> userOpt = userRepository.findByClerkUserId(clerkUserId);
            if (userOpt.isEmpty()) {
                log.warn("User not found for update: {}", clerkUserId);
                // Create user if it doesn't exist (might have missed created event)
                processUserCreated(eventData);
                return;
            }
            
            User user = userOpt.get();
            if (data.has("first_name")) user.setFirstName(data.get("first_name").asText());
            if (data.has("last_name")) user.setLastName(data.get("last_name").asText());
            if (data.has("image_url")) user.setImageUrl(data.get("image_url").asText());
            
            userRepository.save(user);
            log.info("User updated: {}", clerkUserId);
            
            // Store event for audit
            storeUserEvent(clerkUserId, "user.updated", eventData);
            
        } catch (Exception e) {
            log.error("Error processing user.updated event", e);
            throw new RuntimeException("Failed to process user.updated event", e);
        }
    }
    
    @Transactional
    public void processOrganizationCreated(JsonNode eventData) {
        try {
            JsonNode data = eventData.get("data");
            String clerkOrgId = data.get("id").asText();
            
            // Check if organization already exists (idempotency)
            if (organizationRepository.existsByClerkOrgId(clerkOrgId)) {
                log.info("Organization already exists, skipping: {}", clerkOrgId);
                return;
            }
            
            Organization org = Organization.builder()
                .clerkOrgId(clerkOrgId)
                .name(data.has("name") ? data.get("name").asText() : "Unnamed Organization")
                .slug(data.has("slug") ? data.get("slug").asText() : null)
                .imageUrl(data.has("image_url") ? data.get("image_url").asText() : null)
                .build();
            
            organizationRepository.save(org);
            log.info("Organization created: {} ({})", org.getName(), clerkOrgId);
            
            // Store event for audit
            storeOrganizationEvent(clerkOrgId, null, "organization.created", eventData);
            
        } catch (Exception e) {
            log.error("Error processing organization.created event", e);
            throw new RuntimeException("Failed to process organization.created event", e);
        }
    }
    
    @Transactional
    public void processOrganizationMembershipCreated(JsonNode eventData) {
        try {
            JsonNode data = eventData.get("data");
            String clerkMembershipId = data.get("id").asText();
            String clerkOrgId = data.get("organization_id").asText();
            String clerkUserId = data.get("public_user_data").get("user_id").asText();
            String roleName = data.has("role") ? data.get("role").asText() : "USER";
            
            // Check if membership already exists (idempotency)
            if (membershipRepository.findByClerkMembershipId(clerkMembershipId).isPresent()) {
                log.info("Membership already exists, skipping: {}", clerkMembershipId);
                return;
            }
            
            // Find user and organization
            User user = userRepository.findByClerkUserId(clerkUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + clerkUserId));
            Organization org = organizationRepository.findByClerkOrgId(clerkOrgId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + clerkOrgId));
            
            // Find role (default to USER if not found)
            Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseGet(() -> {
                    log.warn("Role not found: {}, defaulting to USER", roleName);
                    return roleRepository.findByName("USER")
                        .orElseThrow(() -> new RuntimeException("Default USER role not found"));
                });
            
            // Check if user already has a membership in this organization
            Optional<Membership> existingMembership = membershipRepository
                .findByUserIdAndOrganizationId(user.getId(), org.getId());
            
            if (existingMembership.isPresent()) {
                // Update existing membership
                Membership membership = existingMembership.get();
                membership.setRole(role);
                membership.setClerkMembershipId(clerkMembershipId);
                membershipRepository.save(membership);
                log.info("Membership updated: user {} in org {} with role {}", clerkUserId, clerkOrgId, roleName);
            } else {
                // Create new membership
                Membership membership = Membership.builder()
                    .user(user)
                    .organization(org)
                    .role(role)
                    .clerkMembershipId(clerkMembershipId)
                    .build();
                
                membershipRepository.save(membership);
                log.info("Membership created: user {} in org {} with role {}", clerkUserId, clerkOrgId, roleName);
            }
            
            // Store event for audit
            storeOrganizationEvent(clerkOrgId, clerkUserId, "organizationMembership.created", eventData);
            
        } catch (Exception e) {
            log.error("Error processing organizationMembership.created event", e);
            throw new RuntimeException("Failed to process organizationMembership.created event", e);
        }
    }
    
    @Transactional
    public void processOrganizationMembershipDeleted(JsonNode eventData) {
        try {
            JsonNode data = eventData.get("data");
            String clerkMembershipId = data.get("id").asText();
            String clerkOrgId = data.has("organization_id") ? data.get("organization_id").asText() : null;
            String clerkUserId = data.has("public_user_data") && data.get("public_user_data").has("user_id")
                ? data.get("public_user_data").get("user_id").asText()
                : null;
            
            Optional<Membership> membershipOpt = membershipRepository.findByClerkMembershipId(clerkMembershipId);
            if (membershipOpt.isPresent()) {
                membershipRepository.delete(membershipOpt.get());
                log.info("Membership deleted: {}", clerkMembershipId);
            } else {
                log.warn("Membership not found for deletion: {}", clerkMembershipId);
            }
            
            // Store event for audit
            storeOrganizationEvent(clerkOrgId, clerkUserId, "organizationMembership.deleted", eventData);
            
        } catch (Exception e) {
            log.error("Error processing organizationMembership.deleted event", e);
            throw new RuntimeException("Failed to process organizationMembership.deleted event", e);
        }
    }
    
    private void storeUserEvent(String clerkUserId, String eventType, JsonNode eventData) {
        try {
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            String clerkEventId = eventData.has("id") ? eventData.get("id").asText() : null;
            
            // Check for duplicate events
            if (clerkEventId != null && userEventRepository.existsByClerkEventId(clerkEventId)) {
                log.debug("Duplicate user event skipped: {}", clerkEventId);
                return;
            }
            
            UserEvent event = UserEvent.builder()
                .clerkUserId(clerkUserId)
                .eventType(eventType)
                .eventData(eventDataJson)
                .clerkEventId(clerkEventId)
                .build();
            
            userEventRepository.save(event);
        } catch (Exception e) {
            log.error("Error storing user event", e);
            // Don't throw - event storage failure shouldn't break webhook processing
        }
    }
    
    private void storeOrganizationEvent(String clerkOrgId, String clerkUserId, String eventType, JsonNode eventData) {
        try {
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            String clerkEventId = eventData.has("id") ? eventData.get("id").asText() : null;
            
            // Check for duplicate events
            if (clerkEventId != null && organizationEventRepository.existsByClerkEventId(clerkEventId)) {
                log.debug("Duplicate organization event skipped: {}", clerkEventId);
                return;
            }
            
            OrganizationEvent event = OrganizationEvent.builder()
                .clerkOrgId(clerkOrgId)
                .clerkUserId(clerkUserId)
                .eventType(eventType)
                .eventData(eventDataJson)
                .clerkEventId(clerkEventId)
                .build();
            
            organizationEventRepository.save(event);
        } catch (Exception e) {
            log.error("Error storing organization event", e);
            // Don't throw - event storage failure shouldn't break webhook processing
        }
    }
}
