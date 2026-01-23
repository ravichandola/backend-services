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
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.get("type").asText();
            JsonNode eventData = event.get("data");
            
            log.info("Received Clerk webhook event: {} (svix-id: {})", eventType, svixId);
            
            // Route to appropriate handler
            switch (eventType) {
                case "user.created":
                    webhookService.processUserCreated(event);
                    break;
                case "user.updated":
                    webhookService.processUserUpdated(event);
                    break;
                case "organization.created":
                    webhookService.processOrganizationCreated(event);
                    break;
                case "organizationMembership.created":
                    webhookService.processOrganizationMembershipCreated(event);
                    break;
                case "organizationMembership.deleted":
                    webhookService.processOrganizationMembershipDeleted(event);
                    break;
                default:
                    // Ignore events we don't need (like email.created, session.*, etc.)
                    // Return 200 OK so Clerk doesn't retry
                    log.debug("Ignoring webhook event type: {} (not needed for user/org sync)", eventType);
                    return ResponseEntity.ok("Event ignored: " + eventType);
            }
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing webhook", e);
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
