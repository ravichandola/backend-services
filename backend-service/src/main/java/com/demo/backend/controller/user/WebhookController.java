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
                    log.warn("Unhandled webhook event type: {}", eventType);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Unhandled event type: " + eventType);
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
            log.warn("Missing required webhook headers");
            return false;
        }
        
        try {
            // Parse signature (format: v1,<signature>)
            String[] signatureParts = svixSignature.split(",");
            if (signatureParts.length != 2 || !signatureParts[0].equals("v1")) {
                log.warn("Invalid signature format");
                return false;
            }
            
            String receivedSignature = signatureParts[1];
            
            // Construct signed content: <id>.<timestamp>.<payload>
            String signedContent = svixId + "." + svixTimestamp + "." + payload;
            
            // Compute expected signature
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);
            
            // Constant-time comparison to prevent timing attacks
            return constantTimeEquals(receivedSignature, expectedSignature);
            
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
