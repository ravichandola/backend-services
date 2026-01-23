package com.demo.backend.controller.user;

import com.demo.backend.service.user.WebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Webhook controller for receiving Clerk webhook events
 * Handles signature verification and routes events to WebhookService
 * 
 * IMPORTANT: This endpoint should be accessible without authentication
 * (configured in Spring Security to allow unauthenticated access)
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;
    
    @Value("${clerk.webhook.secret:}")
    private String webhookSecret;
    
    /**
     * Clerk webhook endpoint
     * Verifies webhook signature and processes events
     */
    @PostMapping("/clerk")
    public ResponseEntity<String> handleClerkWebhook(
            @RequestHeader(value = "svix-id", required = false) String svixId,
            @RequestHeader(value = "svix-timestamp", required = false) String svixTimestamp,
            @RequestHeader(value = "svix-signature", required = false) String svixSignature,
            @RequestBody String payload,
            HttpServletRequest request) {
        
        // Debug: Log all headers to see what we're receiving
        log.debug("All request headers: {}", java.util.Collections.list(request.getHeaderNames()));
        
        // Fallback: Try to get headers directly from request if not found via annotation
        // (Spring may not match case-insensitive headers correctly)
        if (svixId == null) {
            svixId = request.getHeader("Svix-Id");
            log.debug("Tried Svix-Id header, got: {}", svixId != null ? "found" : "null");
        }
        if (svixTimestamp == null) {
            svixTimestamp = request.getHeader("Svix-Timestamp");
            log.debug("Tried Svix-Timestamp header, got: {}", svixTimestamp != null ? "found" : "null");
        }
        if (svixSignature == null) {
            svixSignature = request.getHeader("Svix-Signature");
            log.debug("Tried Svix-Signature header, got: {}", svixSignature != null ? "found" : "null");
        }
        
        // Also try lowercase versions
        if (svixId == null) {
            svixId = request.getHeader("svix-id");
        }
        if (svixTimestamp == null) {
            svixTimestamp = request.getHeader("svix-timestamp");
        }
        if (svixSignature == null) {
            svixSignature = request.getHeader("svix-signature");
        }
        
        log.info("Webhook headers - svixId: {}, svixTimestamp: {}, svixSignature: {}", 
            svixId != null, svixTimestamp != null, svixSignature != null);
        
        try {
            // Verify webhook signature
            if (!verifySignature(svixId, svixTimestamp, svixSignature, payload)) {
                log.warn("Invalid webhook signature. Rejecting request.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid signature");
            }
            
            // Parse webhook payload
            log.info("Parsing webhook payload. Payload length: {} bytes", payload.length());
            JsonNode event = objectMapper.readTree(payload);
            
            if (!event.has("type")) {
                log.error("Webhook payload missing 'type' field. Payload: {}", payload);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Missing 'type' field in webhook payload");
            }
            
            // Add svix-id to event payload if not already present (for event ID tracking)
            if (svixId != null && !event.has("id") && !event.has("event_id")) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) event).put("svix_id", svixId);
                log.debug("Added svix-id to event payload: {}", svixId);
            }
            
            String eventType = event.get("type").asText();
            JsonNode eventData = event.has("data") ? event.get("data") : null;
            
            log.info("Received Clerk webhook event: {} (svix-id: {})", eventType, svixId);
            log.debug("Webhook payload structure - type: {}, has data: {}", eventType, eventData != null);
            
            // Route to appropriate handler
            try {
                switch (eventType) {
                    case "user.created":
                        log.info("Routing to processUserCreated handler");
                        webhookService.processUserCreated(event);
                        log.info("processUserCreated completed successfully");
                        break;
                    case "user.updated":
                        log.info("Routing to processUserUpdated handler");
                        webhookService.processUserUpdated(event);
                        log.info("processUserUpdated completed successfully");
                        break;
                    case "organization.created":
                        log.info("Routing to processOrganizationCreated handler");
                        webhookService.processOrganizationCreated(event);
                        log.info("processOrganizationCreated completed successfully");
                        break;
                    case "organization.updated":
                        log.info("Routing to processOrganizationUpdated handler");
                        webhookService.processOrganizationUpdated(event);
                        log.info("processOrganizationUpdated completed successfully");
                        break;
                    case "organization.deleted":
                        log.info("Routing to processOrganizationDeleted handler");
                        webhookService.processOrganizationDeleted(event);
                        log.info("processOrganizationDeleted completed successfully");
                        break;
                    case "organizationMembership.created":
                        log.info("Routing to processOrganizationMembershipCreated handler");
                        webhookService.processOrganizationMembershipCreated(event);
                        log.info("processOrganizationMembershipCreated completed successfully");
                        break;
                    case "organizationMembership.updated":
                        log.info("Routing to processOrganizationMembershipUpdated handler");
                        webhookService.processOrganizationMembershipUpdated(event);
                        log.info("processOrganizationMembershipUpdated completed successfully");
                        break;
                    case "organizationMembership.deleted":
                        log.info("Routing to processOrganizationMembershipDeleted handler");
                        webhookService.processOrganizationMembershipDeleted(event);
                        log.info("processOrganizationMembershipDeleted completed successfully");
                        break;
                    case "email.created":
                        log.info("Routing to processEmailCreated handler");
                        webhookService.processEmailCreated(event);
                        log.info("processEmailCreated completed successfully");
                        break;
                    case "role.created":
                        log.info("Routing to processRoleCreated handler");
                        webhookService.processRoleCreated(event);
                        log.info("processRoleCreated completed successfully");
                        break;
                    case "role.updated":
                        log.info("Routing to processRoleUpdated handler");
                        webhookService.processRoleUpdated(event);
                        log.info("processRoleUpdated completed successfully");
                        break;
                    case "role.deleted":
                        log.info("Routing to processRoleDeleted handler");
                        webhookService.processRoleDeleted(event);
                        log.info("processRoleDeleted completed successfully");
                        break;
                    case "payment.attempt":
                    case "paymentAttempt":
                        log.info("Routing to processPaymentAttempt handler");
                        webhookService.processPaymentAttempt(event);
                        log.info("processPaymentAttempt completed successfully");
                        break;
                    default:
                        // Ignore events we don't need (like session.*, etc.)
                        // Return 200 OK so Clerk doesn't retry
                        log.debug("Ignoring webhook event type: {} (not needed for user/org sync)", eventType);
                        return ResponseEntity.ok("Event ignored: " + eventType);
                }
                
                log.info("Webhook event {} processed successfully", eventType);
                return ResponseEntity.ok("Webhook processed successfully");
                
            } catch (RuntimeException e) {
                log.error("Error processing webhook event {}: {}", eventType, e.getMessage(), e);
                // Still return 200 to prevent Clerk from retrying (if it's a data issue, not a system issue)
                // But log the error for debugging
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error parsing or processing webhook. Payload: {}", payload, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook: " + e.getMessage());
        }
    }
    
    /**
     * Verify Clerk webhook signature using Svix signature verification
     * 
     * Clerk uses Svix for webhook delivery, which signs webhooks using HMAC SHA256
     * Signature format: v1,<signature>
     * 
     * @param svixId The webhook ID header
     * @param svixTimestamp The timestamp header
     * @param svixSignature The signature header (format: v1,<signature>)
     * @param payload The raw request body
     * @return true if signature is valid, false otherwise
     */
    private boolean verifySignature(String svixId, String svixTimestamp, String svixSignature, String payload) {
        // Skip verification if webhook secret is not configured (for development)
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            log.warn("Webhook secret not configured. Skipping signature verification.");
            return true; // Allow in development, but should be false in production
        }
        
        if (svixId == null || svixTimestamp == null || svixSignature == null) {
            log.warn("Missing required webhook headers. svixId: {}, svixTimestamp: {}, svixSignature: {}", 
                svixId != null, svixTimestamp != null, svixSignature != null);
            return false;
        }
        
        try {
            // Decode Clerk webhook secret (format: whsec_<base64-encoded-secret>)
            byte[] secretBytes;
            if (webhookSecret.startsWith("whsec_")) {
                // Remove 'whsec_' prefix and base64 decode
                String secretBase64 = webhookSecret.substring(6); // Remove "whsec_" prefix
                secretBytes = Base64.getDecoder().decode(secretBase64);
                log.debug("Decoded webhook secret from whsec_ format. Secret length: {} bytes", secretBytes.length);
            } else {
                // Assume secret is already in raw format (for backward compatibility)
                secretBytes = webhookSecret.getBytes(StandardCharsets.UTF_8);
                log.debug("Using webhook secret as-is (no whsec_ prefix). Secret length: {} bytes", secretBytes.length);
            }
            
            log.debug("Verifying webhook signature. Secret length: {} bytes, svixId: {}, svixTimestamp: {}", 
                secretBytes.length, svixId, svixTimestamp);
            
            // Parse signature (format: v1,<signature>)
            String[] signatureParts = svixSignature.split(",");
            if (signatureParts.length != 2 || !signatureParts[0].equals("v1")) {
                log.warn("Invalid signature format. Expected 'v1,<signature>', got: {}", svixSignature.substring(0, Math.min(50, svixSignature.length())));
                return false;
            }
            
            String receivedSignature = signatureParts[1];
            
            // Construct signed content: <id>.<timestamp>.<payload>
            String signedContent = svixId + "." + svixTimestamp + "." + payload;
            
            // Compute expected signature using decoded secret
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);
            
            log.debug("Signature comparison - Received (first 20): {}, Expected (first 20): {}", 
                receivedSignature.substring(0, Math.min(20, receivedSignature.length())),
                expectedSignature.substring(0, Math.min(20, expectedSignature.length())));
            
            // Constant-time comparison to prevent timing attacks
            boolean isValid = constantTimeEquals(receivedSignature, expectedSignature);
            
            if (!isValid) {
                log.warn("Signature mismatch. Received length: {}, Expected length: {}", 
                    receivedSignature.length(), expectedSignature.length());
            }
            
            return isValid;
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }
    
    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
