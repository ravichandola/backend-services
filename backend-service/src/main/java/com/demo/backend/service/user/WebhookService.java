package com.demo.backend.service.user;

import com.demo.backend.entity.user.*;
import com.demo.backend.repository.user.*;
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
            log.info("Processing user.created event. Full payload: {}", eventData.toPrettyString());
            
            JsonNode data = eventData.get("data");
            if (data == null) {
                log.error("user.created event missing 'data' field. Event structure: {}", eventData.toPrettyString());
                throw new RuntimeException("user.created event missing 'data' field");
            }
            
            String clerkUserId = data.has("id") ? data.get("id").asText() : null;
            if (clerkUserId == null) {
                log.error("user.created event missing 'id' field in data. Data structure: {}", data.toPrettyString());
                throw new RuntimeException("user.created event missing 'id' field");
            }
            
            log.info("Processing user.created for clerkUserId: {}", clerkUserId);
            
            // Extract email - try multiple possible locations
            String email = null;
            if (data.has("email_addresses") && data.get("email_addresses").isArray() && data.get("email_addresses").size() > 0) {
                JsonNode emailAddresses = data.get("email_addresses");
                JsonNode firstEmail = emailAddresses.get(0);
                if (firstEmail.has("email_address")) {
                    email = firstEmail.get("email_address").asText();
                } else if (firstEmail.has("email")) {
                    email = firstEmail.get("email").asText();
                } else if (firstEmail.isTextual()) {
                    email = firstEmail.asText();
                }
            } else if (data.has("primary_email_address")) {
                email = data.get("primary_email_address").asText();
            } else if (data.has("email")) {
                email = data.get("email").asText();
            }
            
            log.info("Extracted email for user {}: {}", clerkUserId, email != null ? email : "null");
            
            if (email == null || email.isEmpty()) {
                log.warn("User created event missing email. clerkUserId: {}, data structure: {}", 
                    clerkUserId, data.toPrettyString());
                // Don't return early - still store the event for audit
                // But we can't create user without email
                storeUserEvent(clerkUserId, "user.created", eventData);
                log.warn("Skipping user creation due to missing email. Event stored for audit.");
                return;
            }
            
            // Check if user already exists (idempotency)
            if (userRepository.existsByClerkUserId(clerkUserId)) {
                log.info("User already exists, skipping: {} ({})", email, clerkUserId);
                // Still store event for audit
                storeUserEvent(clerkUserId, "user.created", eventData);
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
            log.info("User created successfully: {} ({})", email, clerkUserId);
            
            // Store event for audit
            storeUserEvent(clerkUserId, "user.created", eventData);
            log.info("User event stored in audit table for: {}", clerkUserId);
            
        } catch (Exception e) {
            log.error("Error processing user.created event. Event payload: {}", 
                eventData != null ? eventData.toPrettyString() : "null", e);
            throw new RuntimeException("Failed to process user.created event: " + e.getMessage(), e);
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
            log.info("Processing organization.created event. Full payload: {}", eventData.toPrettyString());
            
            JsonNode data = eventData.get("data");
            if (data == null) {
                log.error("organization.created event missing 'data' field. Event structure: {}", eventData.toPrettyString());
                throw new RuntimeException("organization.created event missing 'data' field");
            }
            
            String clerkOrgId = data.has("id") ? data.get("id").asText() : null;
            if (clerkOrgId == null) {
                log.error("organization.created event missing 'id' field in data. Data structure: {}", data.toPrettyString());
                throw new RuntimeException("organization.created event missing 'id' field");
            }
            
            log.info("Processing organization.created for clerkOrgId: {}", clerkOrgId);
            
            // Check if organization already exists (idempotency)
            if (organizationRepository.existsByClerkOrgId(clerkOrgId)) {
                log.info("Organization already exists, skipping: {}", clerkOrgId);
                // Still store event for audit
                storeOrganizationEvent(clerkOrgId, null, "organization.created", eventData);
                return;
            }
            
            Organization org = Organization.builder()
                .clerkOrgId(clerkOrgId)
                .name(data.has("name") ? data.get("name").asText() : "Unnamed Organization")
                .slug(data.has("slug") ? data.get("slug").asText() : null)
                .imageUrl(data.has("image_url") ? data.get("image_url").asText() : null)
                .build();
            
            organizationRepository.save(org);
            log.info("Organization created successfully: {} ({})", org.getName(), clerkOrgId);
            
            // Store event for audit
            storeOrganizationEvent(clerkOrgId, null, "organization.created", eventData);
            log.info("Organization event stored in audit table for: {}", clerkOrgId);
            
        } catch (Exception e) {
            log.error("Error processing organization.created event. Event payload: {}", 
                eventData != null ? eventData.toPrettyString() : "null", e);
            throw new RuntimeException("Failed to process organization.created event: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public void processOrganizationUpdated(JsonNode eventData) {
        try {
            log.info("Processing organization.updated event. Full payload: {}", eventData.toPrettyString());
            
            JsonNode data = eventData.get("data");
            if (data == null) {
                log.error("organization.updated event missing 'data' field. Event structure: {}", eventData.toPrettyString());
                throw new RuntimeException("organization.updated event missing 'data' field");
            }
            
            String clerkOrgId = data.has("id") ? data.get("id").asText() : null;
            if (clerkOrgId == null) {
                log.error("organization.updated event missing 'id' field in data. Data structure: {}", data.toPrettyString());
                throw new RuntimeException("organization.updated event missing 'id' field");
            }
            
            log.info("Processing organization.updated for clerkOrgId: {}", clerkOrgId);
            
            Optional<Organization> orgOpt = organizationRepository.findByClerkOrgId(clerkOrgId);
            if (orgOpt.isEmpty()) {
                log.warn("Organization not found for update: {}. Creating new organization.", clerkOrgId);
                // Create organization if it doesn't exist (might have missed created event)
                processOrganizationCreated(eventData);
                return;
            }
            
            Organization org = orgOpt.get();
            
            // Update fields if present in webhook data
            if (data.has("name")) {
                org.setName(data.get("name").asText());
                log.debug("Updated organization name: {}", org.getName());
            }
            if (data.has("slug")) {
                org.setSlug(data.get("slug").asText());
                log.debug("Updated organization slug: {}", org.getSlug());
            }
            if (data.has("image_url")) {
                org.setImageUrl(data.get("image_url").asText());
                log.debug("Updated organization imageUrl");
            }
            
            organizationRepository.save(org);
            log.info("Organization updated successfully: {} ({})", org.getName(), clerkOrgId);
            
            // Store event for audit
            storeOrganizationEvent(clerkOrgId, null, "organization.updated", eventData);
            log.info("Organization updated event stored in audit table for: {}", clerkOrgId);
            
        } catch (Exception e) {
            log.error("Error processing organization.updated event. Event payload: {}", 
                eventData != null ? eventData.toPrettyString() : "null", e);
            throw new RuntimeException("Failed to process organization.updated event: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public void processOrganizationDeleted(JsonNode eventData) {
        try {
            log.info("Processing organization.deleted event. Full payload: {}", eventData.toPrettyString());
            
            JsonNode data = eventData.get("data");
            if (data == null) {
                log.error("organization.deleted event missing 'data' field. Event structure: {}", eventData.toPrettyString());
                throw new RuntimeException("organization.deleted event missing 'data' field");
            }
            
            String clerkOrgId = data.has("id") ? data.get("id").asText() : null;
            if (clerkOrgId == null) {
                log.error("organization.deleted event missing 'id' field in data. Data structure: {}", data.toPrettyString());
                throw new RuntimeException("organization.deleted event missing 'id' field");
            }
            
            log.info("Processing organization.deleted for clerkOrgId: {}", clerkOrgId);
            
            Optional<Organization> orgOpt = organizationRepository.findByClerkOrgId(clerkOrgId);
            if (orgOpt.isPresent()) {
                Organization org = orgOpt.get();
                String orgName = org.getName();
                
                // Delete all memberships for this organization first (cascade might handle this, but explicit is better)
                membershipRepository.deleteAll(membershipRepository.findByOrganizationId(org.getId()));
                log.info("Deleted all memberships for organization: {}", clerkOrgId);
                
                // Delete the organization
                organizationRepository.delete(org);
                log.info("Organization deleted successfully: {} ({})", orgName, clerkOrgId);
            } else {
                log.warn("Organization not found for deletion: {}. It may have already been deleted.", clerkOrgId);
            }
            
            // Store event for audit (even if org wasn't found, we still want to track the deletion attempt)
            storeOrganizationEvent(clerkOrgId, null, "organization.deleted", eventData);
            log.info("Organization deleted event stored in audit table for: {}", clerkOrgId);
            
        } catch (Exception e) {
            log.error("Error processing organization.deleted event. Event payload: {}", 
                eventData != null ? eventData.toPrettyString() : "null", e);
            throw new RuntimeException("Failed to process organization.deleted event: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public void processOrganizationMembershipCreated(JsonNode eventData) {
        try {
            JsonNode data = eventData.get("data");
            String clerkMembershipId = data.get("id").asText();
            String clerkOrgId = data.get("organization_id").asText();
            String clerkUserId = data.get("public_user_data").get("user_id").asText();
            
            // Try multiple possible locations for role in Clerk webhook payload
            final String roleName;
            if (data.has("role")) {
                roleName = data.get("role").asText();
                log.debug("Found role in data.role: {}", roleName);
            } else if (data.has("public_metadata") && data.get("public_metadata").has("role")) {
                roleName = data.get("public_metadata").get("role").asText();
                log.debug("Found role in data.public_metadata.role: {}", roleName);
            } else if (data.has("public_user_data") && data.get("public_user_data").has("role")) {
                roleName = data.get("public_user_data").get("role").asText();
                log.debug("Found role in data.public_user_data.role: {}", roleName);
            } else {
                roleName = "USER"; // default
                log.warn("Role not found in webhook payload, defaulting to USER. Payload structure: {}", data.toPrettyString());
            }
            
            log.info("Processing organizationMembership.created - user: {}, org: {}, role: {}", 
                clerkUserId, clerkOrgId, roleName);
            
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
            final String finalRoleName = roleName; // Make effectively final for lambda
            Role role = roleRepository.findByName(finalRoleName.toUpperCase())
                .orElseGet(() -> {
                    log.warn("Role not found: {}, defaulting to USER", finalRoleName);
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
    public void processOrganizationMembershipUpdated(JsonNode eventData) {
        try {
            log.info("Processing organizationMembership.updated event. Full payload: {}", eventData.toPrettyString());
            
            JsonNode data = eventData.get("data");
            if (data == null) {
                log.error("organizationMembership.updated event missing 'data' field. Event structure: {}", eventData.toPrettyString());
                throw new RuntimeException("organizationMembership.updated event missing 'data' field");
            }
            
            String clerkMembershipId = data.has("id") ? data.get("id").asText() : null;
            if (clerkMembershipId == null) {
                log.error("organizationMembership.updated event missing 'id' field in data. Data structure: {}", data.toPrettyString());
                throw new RuntimeException("organizationMembership.updated event missing 'id' field");
            }
            
            String clerkOrgId = data.has("organization_id") ? data.get("organization_id").asText() : null;
            String clerkUserId = null;
            
            if (data.has("public_user_data") && data.get("public_user_data").has("user_id")) {
                clerkUserId = data.get("public_user_data").get("user_id").asText();
            } else if (data.has("user_id")) {
                clerkUserId = data.get("user_id").asText();
            }
            
            if (clerkOrgId == null) {
                log.error("organizationMembership.updated event missing 'organization_id' field");
                throw new RuntimeException("organizationMembership.updated event missing 'organization_id' field");
            }
            
            if (clerkUserId == null) {
                log.error("organizationMembership.updated event missing 'user_id' field");
                throw new RuntimeException("organizationMembership.updated event missing 'user_id' field");
            }
            
            // Try multiple possible locations for role in Clerk webhook payload
            final String roleName;
            if (data.has("role")) {
                roleName = data.get("role").asText();
                log.debug("Found role in data.role: {}", roleName);
            } else if (data.has("public_metadata") && data.get("public_metadata").has("role")) {
                roleName = data.get("public_metadata").get("role").asText();
                log.debug("Found role in data.public_metadata.role: {}", roleName);
            } else if (data.has("public_user_data") && data.get("public_user_data").has("role")) {
                roleName = data.get("public_user_data").get("role").asText();
                log.debug("Found role in data.public_user_data.role: {}", roleName);
            } else {
                roleName = "USER"; // default
                log.warn("Role not found in webhook payload, defaulting to USER. Payload structure: {}", data.toPrettyString());
            }
            
            log.info("Processing organizationMembership.updated - membership: {}, user: {}, org: {}, role: {}", 
                clerkMembershipId, clerkUserId, clerkOrgId, roleName);
            
            // Find membership by clerkMembershipId first
            Optional<Membership> membershipOpt = membershipRepository.findByClerkMembershipId(clerkMembershipId);
            
            if (membershipOpt.isEmpty()) {
                log.warn("Membership not found for update: {}. Creating new membership.", clerkMembershipId);
                // Create membership if it doesn't exist (might have missed created event)
                processOrganizationMembershipCreated(eventData);
                return;
            }
            
            Membership membership = membershipOpt.get();
            
            // Find role (default to USER if not found)
            final String finalRoleName = roleName; // Make effectively final for lambda
            Role role = roleRepository.findByName(finalRoleName.toUpperCase())
                .orElseGet(() -> {
                    log.warn("Role not found: {}, defaulting to USER", finalRoleName);
                    return roleRepository.findByName("USER")
                        .orElseThrow(() -> new RuntimeException("Default USER role not found"));
                });
            
            // Update membership role
            Role oldRole = membership.getRole();
            membership.setRole(role);
            membership.setClerkMembershipId(clerkMembershipId); // Update in case it changed
            
            membershipRepository.save(membership);
            log.info("Membership updated successfully: user {} in org {} from role {} to {}", 
                clerkUserId, clerkOrgId, oldRole != null ? oldRole.getName() : "null", roleName);
            
            // Store event for audit
            storeOrganizationEvent(clerkOrgId, clerkUserId, "organizationMembership.updated", eventData);
            log.info("Organization membership updated event stored in audit table");
            
        } catch (Exception e) {
            log.error("Error processing organizationMembership.updated event. Event payload: {}", 
                eventData != null ? eventData.toPrettyString() : "null", e);
            throw new RuntimeException("Failed to process organizationMembership.updated event: " + e.getMessage(), e);
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
            
            // Extract clerkUserId from event data if not provided
            if (clerkUserId == null || clerkUserId.isEmpty()) {
                JsonNode data = eventData.has("data") ? eventData.get("data") : null;
                if (data != null) {
                    if (data.has("id")) {
                        clerkUserId = data.get("id").asText();
                    } else if (data.has("user_id")) {
                        clerkUserId = data.get("user_id").asText();
                    }
                }
                if (clerkUserId != null) {
                    log.debug("Extracted clerkUserId from event data: {}", clerkUserId);
                }
            }
            
            // Extract event ID from multiple possible locations
            String clerkEventId = null;
            // 1. Check svix_id (added from header - most reliable)
            if (eventData.has("svix_id")) {
                clerkEventId = eventData.get("svix_id").asText();
                log.debug("Using svix_id as event ID: {}", clerkEventId);
            }
            // 2. Check root level id (event ID)
            else if (eventData.has("id")) {
                clerkEventId = eventData.get("id").asText();
            }
            // 3. Check for event_id field
            else if (eventData.has("event_id")) {
                clerkEventId = eventData.get("event_id").asText();
            }
            // 4. Check instance_id (fallback, though not ideal)
            else if (eventData.has("instance_id")) {
                // instance_id is not unique per event, but better than null
                String instanceId = eventData.get("instance_id").asText();
                String timestamp = eventData.has("timestamp") ? eventData.get("timestamp").asText() : String.valueOf(System.currentTimeMillis());
                clerkEventId = instanceId + "_" + timestamp;
                log.debug("Using instance_id + timestamp as event ID: {}", clerkEventId);
            }
            
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
            log.debug("User event stored - type: {}, userId: {}, eventId: {}", eventType, clerkUserId, clerkEventId);
        } catch (Exception e) {
            log.error("Error storing user event", e);
            // Don't throw - event storage failure shouldn't break webhook processing
        }
    }
    
    private void storeOrganizationEvent(String clerkOrgId, String clerkUserId, String eventType, JsonNode eventData) {
        try {
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            
            // Extract clerkOrgId from event data if not provided
            if (clerkOrgId == null || clerkOrgId.isEmpty()) {
                JsonNode data = eventData.has("data") ? eventData.get("data") : null;
                if (data != null) {
                    if (data.has("id")) {
                        clerkOrgId = data.get("id").asText();
                    } else if (data.has("organization_id")) {
                        clerkOrgId = data.get("organization_id").asText();
                    } else if (data.has("organization") && data.get("organization").has("id")) {
                        clerkOrgId = data.get("organization").get("id").asText();
                    }
                }
                if (clerkOrgId != null) {
                    log.debug("Extracted clerkOrgId from event data: {}", clerkOrgId);
                }
            }
            
            // Extract clerkUserId from event data if not provided
            if (clerkUserId == null || clerkUserId.isEmpty()) {
                JsonNode data = eventData.has("data") ? eventData.get("data") : null;
                
                // Try multiple locations for user_id
                if (data != null) {
                    // 1. Check public_user_data.user_id (common in membership events)
                    if (data.has("public_user_data") && data.get("public_user_data").has("user_id")) {
                        clerkUserId = data.get("public_user_data").get("user_id").asText();
                    }
                    // 2. Check user_id directly
                    else if (data.has("user_id")) {
                        clerkUserId = data.get("user_id").asText();
                    }
                    // 3. Check user.id
                    else if (data.has("user") && data.get("user").has("id")) {
                        clerkUserId = data.get("user").get("id").asText();
                    }
                    // 4. Check created_by or updated_by fields
                    else if (data.has("created_by")) {
                        clerkUserId = data.get("created_by").asText();
                    }
                    else if (data.has("updated_by")) {
                        clerkUserId = data.get("updated_by").asText();
                    }
                    // 5. Check public_metadata for user_id
                    else if (data.has("public_metadata") && data.get("public_metadata").has("user_id")) {
                        clerkUserId = data.get("public_metadata").get("user_id").asText();
                    }
                    // 6. Check private_metadata for user_id
                    else if (data.has("private_metadata") && data.get("private_metadata").has("user_id")) {
                        clerkUserId = data.get("private_metadata").get("user_id").asText();
                    }
                }
                
                // Also check root level (outside data object)
                if ((clerkUserId == null || clerkUserId.isEmpty()) && eventData.has("user_id")) {
                    clerkUserId = eventData.get("user_id").asText();
                }
                else if ((clerkUserId == null || clerkUserId.isEmpty()) && eventData.has("created_by")) {
                    clerkUserId = eventData.get("created_by").asText();
                }
                else if ((clerkUserId == null || clerkUserId.isEmpty()) && eventData.has("updated_by")) {
                    clerkUserId = eventData.get("updated_by").asText();
                }
                
                if (clerkUserId != null && !clerkUserId.isEmpty()) {
                    log.debug("Extracted clerkUserId from event data: {}", clerkUserId);
                } else {
                    log.debug("No clerkUserId found in event data for event type: {}", eventType);
                }
            }
            
            // Extract event ID from multiple possible locations (for deduplication)
            String clerkEventId = null;
            // 1. Check svix_id (added from header - most reliable)
            if (eventData.has("svix_id")) {
                clerkEventId = eventData.get("svix_id").asText();
                log.debug("Using svix_id as event ID: {}", clerkEventId);
            }
            // 2. Check root level id (event ID)
            else if (eventData.has("id")) {
                clerkEventId = eventData.get("id").asText();
            }
            // 3. Check for event_id field
            else if (eventData.has("event_id")) {
                clerkEventId = eventData.get("event_id").asText();
            }
            // 4. Check instance_id + timestamp (fallback, though not ideal)
            else if (eventData.has("instance_id")) {
                // instance_id is not unique per event, but better than null
                String instanceId = eventData.get("instance_id").asText();
                String timestamp = eventData.has("timestamp") ? eventData.get("timestamp").asText() : String.valueOf(System.currentTimeMillis());
                clerkEventId = instanceId + "_" + timestamp;
                log.debug("Using instance_id + timestamp as event ID: {}", clerkEventId);
            }
            // Note: We don't use data.id as that's usually the resource ID (org/user ID), not the event ID
            
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
            log.debug("Organization event stored - type: {}, orgId: {}, userId: {}", eventType, clerkOrgId, clerkUserId);
        } catch (Exception e) {
            log.error("Error storing organization event", e);
            // Don't throw - event storage failure shouldn't break webhook processing
        }
    }
    
    /**
     * Process email.created webhook event
     * Stores email creation events in user_events table for audit
     */
    @Transactional
    public void processEmailCreated(JsonNode eventData) {
        try {
            log.info("Processing email.created event. Full payload: {}", eventData.toPrettyString());
            
            JsonNode data = eventData.get("data");
            if (data == null) {
                log.error("email.created event missing 'data' field");
                return;
            }
            
            // Extract user ID from email data
            String clerkUserId = null;
            if (data.has("user_id")) {
                clerkUserId = data.get("user_id").asText();
            } else if (data.has("user") && data.get("user").has("id")) {
                clerkUserId = data.get("user").get("id").asText();
            }
            
            if (clerkUserId == null) {
                log.warn("email.created event missing user_id. Event stored with null clerk_user_id");
                // Still store the event for audit
                storeUserEvent("unknown", "email.created", eventData);
                return;
            }
            
            log.info("Processing email.created for user: {}", clerkUserId);
            
            // Store event for audit (email.created is informational, no DB updates needed)
            storeUserEvent(clerkUserId, "email.created", eventData);
            log.info("Email created event stored in audit table for user: {}", clerkUserId);
            
        } catch (Exception e) {
            log.error("Error processing email.created event. Event payload: {}", 
                eventData != null ? eventData.toPrettyString() : "null", e);
            // Don't throw - email.created is informational, shouldn't break webhook processing
        }
    }
    
    /**
     * Process role.created webhook event
     * Note: Roles in our database are static (ADMIN, USER), but we store Clerk role events for audit
     */
    @Transactional
    public void processRoleCreated(JsonNode eventData) {
        try {
            log.info("Processing role.created event. Full payload: {}", eventData.toPrettyString());
            
            JsonNode data = eventData.get("data");
            if (data == null) {
                log.error("role.created event missing 'data' field. Event structure: {}", eventData.toPrettyString());
                return;
            }
            
            // Extract role information
            String roleName = null;
            if (data.has("name")) {
                roleName = data.get("name").asText();
            } else if (data.has("key")) {
                roleName = data.get("key").asText();
            }
            
            // Extract user ID and org ID from role data
            String clerkUserId = null;
            String clerkOrgId = null;
            
            if (data.has("user_id")) {
                clerkUserId = data.get("user_id").asText();
            } else if (data.has("user") && data.get("user").has("id")) {
                clerkUserId = data.get("user").get("id").asText();
            }
            
            if (data.has("organization_id")) {
                clerkOrgId = data.get("organization_id").asText();
            } else if (data.has("organization") && data.get("organization").has("id")) {
                clerkOrgId = data.get("organization").get("id").asText();
            }
            
            log.info("Processing role.created - role: {}, user: {}, org: {}", roleName, clerkUserId, clerkOrgId);
            
            // Optionally create role in database if it doesn't exist (for dynamic roles)
            if (roleName != null && !roleName.isEmpty()) {
                Optional<Role> existingRole = roleRepository.findByName(roleName.toUpperCase());
                if (existingRole.isEmpty()) {
                    log.info("Creating new role in database: {}", roleName.toUpperCase());
                    Role newRole = Role.builder()
                        .name(roleName.toUpperCase())
                        .description("Role created from Clerk webhook")
                        .build();
                    roleRepository.save(newRole);
                    log.info("Role created successfully: {}", roleName.toUpperCase());
                } else {
                    log.debug("Role already exists in database: {}", roleName.toUpperCase());
                }
            }
            
            // Store in appropriate audit table
            if (clerkOrgId != null) {
                storeOrganizationEvent(clerkOrgId, clerkUserId, "role.created", eventData);
                log.info("Role created event stored in organization_events table");
            } else if (clerkUserId != null) {
                storeUserEvent(clerkUserId, "role.created", eventData);
                log.info("Role created event stored in user_events table");
            } else {
                log.warn("Role created event missing both user_id and organization_id. Storing with unknown user");
                storeUserEvent("unknown", "role.created", eventData);
            }
            
        } catch (Exception e) {
            log.error("Error processing role.created event. Event payload: {}", 
                eventData != null ? eventData.toPrettyString() : "null", e);
            // Don't throw - role events are informational, shouldn't break webhook processing
        }
    }
    
    /**
     * Process role.updated webhook event
     * Updates role information if role exists in database
     */
    @Transactional
    public void processRoleUpdated(JsonNode eventData) {
        try {
            log.info("Processing role.updated event. Full payload: {}", eventData.toPrettyString());
            
            JsonNode data = eventData.get("data");
            if (data == null) {
                log.error("role.updated event missing 'data' field. Event structure: {}", eventData.toPrettyString());
                return;
            }
            
            // Extract role information
            String roleName = null;
            if (data.has("name")) {
                roleName = data.get("name").asText();
            } else if (data.has("key")) {
                roleName = data.get("key").asText();
            } else if (data.has("id")) {
                // If only ID is present, try to find role by ID
                String roleId = data.get("id").asText();
                log.debug("Role updated event has ID but no name: {}", roleId);
            }
            
            // Extract user ID and org ID from role data
            String clerkUserId = null;
            String clerkOrgId = null;
            
            if (data.has("user_id")) {
                clerkUserId = data.get("user_id").asText();
            } else if (data.has("user") && data.get("user").has("id")) {
                clerkUserId = data.get("user").get("id").asText();
            }
            
            if (data.has("organization_id")) {
                clerkOrgId = data.get("organization_id").asText();
            } else if (data.has("organization") && data.get("organization").has("id")) {
                clerkOrgId = data.get("organization").get("id").asText();
            }
            
            log.info("Processing role.updated - role: {}, user: {}, org: {}", roleName, clerkUserId, clerkOrgId);
            
            // Update role in database if it exists
            if (roleName != null && !roleName.isEmpty()) {
                Optional<Role> roleOpt = roleRepository.findByName(roleName.toUpperCase());
                if (roleOpt.isPresent()) {
                    Role role = roleOpt.get();
                    if (data.has("description")) {
                        role.setDescription(data.get("description").asText());
                        roleRepository.save(role);
                        log.info("Role updated successfully: {}", roleName.toUpperCase());
                    }
                } else {
                    log.debug("Role not found in database for update: {}. Creating new role.", roleName.toUpperCase());
                    // Create role if it doesn't exist
                    processRoleCreated(eventData);
                }
            }
            
            // Store in appropriate audit table
            if (clerkOrgId != null) {
                storeOrganizationEvent(clerkOrgId, clerkUserId, "role.updated", eventData);
                log.info("Role updated event stored in organization_events table");
            } else if (clerkUserId != null) {
                storeUserEvent(clerkUserId, "role.updated", eventData);
                log.info("Role updated event stored in user_events table");
            } else {
                log.warn("Role updated event missing both user_id and organization_id. Storing with unknown user");
                storeUserEvent("unknown", "role.updated", eventData);
            }
            
        } catch (Exception e) {
            log.error("Error processing role.updated event. Event payload: {}", 
                eventData != null ? eventData.toPrettyString() : "null", e);
            // Don't throw - role events are informational, shouldn't break webhook processing
        }
    }
    
    /**
     * Process role.deleted webhook event
     * Note: We typically don't delete roles from database (ADMIN, USER are static),
     * but we store the event for audit purposes
     */
    @Transactional
    public void processRoleDeleted(JsonNode eventData) {
        try {
            log.info("Processing role.deleted event. Full payload: {}", eventData.toPrettyString());
            
            JsonNode data = eventData.get("data");
            if (data == null) {
                log.error("role.deleted event missing 'data' field. Event structure: {}", eventData.toPrettyString());
                return;
            }
            
            // Extract role information
            String roleName = null;
            if (data.has("name")) {
                roleName = data.get("name").asText();
            } else if (data.has("key")) {
                roleName = data.get("key").asText();
            }
            
            // Extract user ID and org ID from role data
            String clerkUserId = null;
            String clerkOrgId = null;
            
            if (data.has("user_id")) {
                clerkUserId = data.get("user_id").asText();
            } else if (data.has("user") && data.get("user").has("id")) {
                clerkUserId = data.get("user").get("id").asText();
            }
            
            if (data.has("organization_id")) {
                clerkOrgId = data.get("organization_id").asText();
            } else if (data.has("organization") && data.get("organization").has("id")) {
                clerkOrgId = data.get("organization").get("id").asText();
            }
            
            log.info("Processing role.deleted - role: {}, user: {}, org: {}", roleName, clerkUserId, clerkOrgId);
            
            // Note: We typically don't delete roles from database (ADMIN, USER are static)
            // But if it's a custom role, we could delete it
            if (roleName != null && !roleName.isEmpty()) {
                Optional<Role> roleOpt = roleRepository.findByName(roleName.toUpperCase());
                if (roleOpt.isPresent()) {
                    Role role = roleOpt.get();
                    // Only delete if it's not ADMIN or USER (static roles)
                    if (!"ADMIN".equals(role.getName()) && !"USER".equals(role.getName())) {
                        roleRepository.delete(role);
                        log.info("Role deleted from database: {}", roleName.toUpperCase());
                    } else {
                        log.info("Skipping deletion of static role: {}", roleName.toUpperCase());
                    }
                } else {
                    log.debug("Role not found in database for deletion: {}", roleName.toUpperCase());
                }
            }
            
            // Store in appropriate audit table
            if (clerkOrgId != null) {
                storeOrganizationEvent(clerkOrgId, clerkUserId, "role.deleted", eventData);
                log.info("Role deleted event stored in organization_events table");
            } else if (clerkUserId != null) {
                storeUserEvent(clerkUserId, "role.deleted", eventData);
                log.info("Role deleted event stored in user_events table");
            } else {
                log.warn("Role deleted event missing both user_id and organization_id. Storing with unknown user");
                storeUserEvent("unknown", "role.deleted", eventData);
            }
            
        } catch (Exception e) {
            log.error("Error processing role.deleted event. Event payload: {}", 
                eventData != null ? eventData.toPrettyString() : "null", e);
            // Don't throw - role events are informational, shouldn't break webhook processing
        }
    }
    
    /**
     * Process payment attempt webhook events
     * Stores payment attempt events in user_events table for audit
     */
    @Transactional
    public void processPaymentAttempt(JsonNode eventData) {
        try {
            log.info("Processing payment attempt event. Full payload: {}", eventData.toPrettyString());
            
            JsonNode data = eventData.get("data");
            if (data == null) {
                log.error("payment.attempt event missing 'data' field");
                return;
            }
            
            // Extract user ID from payment attempt data
            String clerkUserId = null;
            if (data.has("user_id")) {
                clerkUserId = data.get("user_id").asText();
            } else if (data.has("user") && data.get("user").has("id")) {
                clerkUserId = data.get("user").get("id").asText();
            } else if (data.has("metadata") && data.get("metadata").has("user_id")) {
                clerkUserId = data.get("metadata").get("user_id").asText();
            }
            
            if (clerkUserId == null) {
                log.warn("payment.attempt event missing user_id. Event stored with null clerk_user_id");
                // Still store the event for audit
                storeUserEvent("unknown", "payment.attempt", eventData);
                return;
            }
            
            log.info("Processing payment attempt for user: {}", clerkUserId);
            
            // Store event for audit (payment.attempt is informational, actual payment data is in payment_order/payment_transaction tables)
            storeUserEvent(clerkUserId, "payment.attempt", eventData);
            log.info("Payment attempt event stored in audit table for user: {}", clerkUserId);
            
        } catch (Exception e) {
            log.error("Error processing payment.attempt event. Event payload: {}", 
                eventData != null ? eventData.toPrettyString() : "null", e);
            // Don't throw - payment.attempt is informational, shouldn't break webhook processing
        }
    }
}
