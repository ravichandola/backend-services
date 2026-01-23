# Webhook Integration Fixes - Clerk Webhook Implementation

This document details the issues encountered during Clerk webhook integration and the solutions implemented to resolve them.

## Overview

The webhook integration enables Clerk to synchronize user and organization data with our backend service. Clerk uses Svix for webhook delivery, which requires proper header handling and signature verification.

## Issues and Solutions

### Issue 1: Missing Svix Headers in Backend Service

**Problem:**
The Svix headers (`Svix-Id`, `Svix-Signature`, `Svix-Timestamp`) were not reaching the backend service from the API Gateway. This prevented signature verification and webhook processing.

**Root Cause:**
Spring Cloud Gateway was not explicitly preserving the Svix headers when forwarding webhook requests to the backend service. While Gateway should forward headers by default, the Svix headers were being dropped during the request forwarding process.

**Solution:**
Modified `JwtAuthenticationFilter` in the API Gateway (lines 71-91) to explicitly preserve Svix headers when forwarding webhook requests:

- Check if path starts with `/api/webhooks`
- Extract `Svix-Id`, `Svix-Signature`, and `Svix-Timestamp` headers from incoming request
- Explicitly add these headers to the modified request using `requestBuilder.header()`
- Forward the modified request to backend service

**Key Code Location:** `JwtAuthenticationFilter.java` - webhook path handling section

**Result:**
Svix headers now successfully reach the backend service, enabling proper signature verification.

---

### Issue 2: Webhook Signature Verification Failure

**Problem:**
Webhook signature verification was failing even with correct webhook secret, causing all webhook requests to be rejected with 401 Unauthorized.

**Root Cause:**
Clerk provides webhook secrets in the format `whsec_<base64-encoded-secret>`. The code was using the secret directly without removing the `whsec_` prefix and base64 decoding it. This caused HMAC signature calculation to fail because the secret format was incorrect.

**Solution:**
Updated `WebhookController.verifySignature()` method (lines 158-169) to properly handle Clerk's webhook secret format:

- Check if secret starts with `whsec_` prefix
- Remove `whsec_` prefix and base64 decode the secret
- Use decoded bytes for HMAC signature calculation
- Fallback to raw format if prefix is not present (backward compatibility)

**Key Code Location:** `WebhookController.java` - `verifySignature()` method, secret decoding section

**Signature Verification Process:**
1. Remove `whsec_` prefix from webhook secret
2. Base64 decode the secret to get raw bytes
3. Construct signed content: `<svix-id>.<svix-timestamp>.<payload>`
4. Calculate HMAC SHA256 using decoded secret
5. Compare with received signature using constant-time comparison

**Result:**
Signature verification now passes correctly, allowing webhook requests to be processed.

---

### Issue 3: Unhandled Event Types Causing Retries

**Problem:**
Clerk sends various webhook event types (e.g., `email.created`, `session.created`, `session.ended`) that our application doesn't need to process. When these events were received, the controller returned 400 Bad Request, causing Clerk to retry the webhook delivery repeatedly.

**Root Cause:**
The webhook controller only handled specific event types (`user.created`, `user.updated`, `organization.created`, `organizationMembership.created`, `organizationMembership.deleted`). Unhandled events were causing exceptions or returning error responses, triggering Clerk's retry mechanism.

**Solution:**
Modified the event routing logic in `WebhookController` (lines 100-121) to return 200 OK for unhandled events:

- Added `default` case in switch statement for unhandled event types
- Return `ResponseEntity.ok()` with "Event ignored" message
- Log ignored events at debug level
- Handled events: `user.created`, `user.updated`, `organization.created`, `organizationMembership.created`, `organizationMembership.deleted`

**Key Code Location:** `WebhookController.java` - `handleClerkWebhook()` method, event routing switch statement

**Result:**
Clerk no longer retries unhandled events, reducing unnecessary webhook traffic and improving system efficiency.

---

### Issue 4: Case Sensitivity in Header Reading

**Problem:**
Spring's `@RequestHeader` annotation is case-sensitive, but HTTP headers are case-insensitive according to the HTTP specification. Svix may send headers with different casing (e.g., `Svix-Id` vs `svix-id`), causing header extraction to fail.

**Root Cause:**
Spring's `@RequestHeader` annotation performs exact case matching. If Svix sends headers with different casing than expected, the headers would be null, causing signature verification to fail.

**Solution:**
Added fallback logic in `WebhookController` (lines 46-79) to read headers directly from `HttpServletRequest`, which performs case-insensitive header lookup:

- Use `@RequestHeader` with `required = false` for initial header extraction
- Fallback to `request.getHeader()` if annotation-based extraction returns null
- Try both capitalized (`Svix-Id`) and lowercase (`svix-id`) header names
- Apply same fallback pattern for all three Svix headers

**Key Code Location:** `WebhookController.java` - `handleClerkWebhook()` method, header extraction section

**Result:**
Headers are now correctly extracted regardless of casing, ensuring reliable webhook processing.

---

## Security Considerations

### Signature Verification

All webhook requests are verified using HMAC SHA256 signature verification:

1. **Constant-Time Comparison**: Uses constant-time string comparison to prevent timing attacks
2. **Secret Handling**: Properly decodes Clerk's `whsec_` prefixed secrets
3. **Header Validation**: Ensures all required headers are present before verification

### Webhook Secret Configuration

The webhook secret should be configured via environment variable:
```bash
CLERK_WEBHOOK_SECRET=whsec_<your-secret>
```

**Important:** Never commit webhook secrets to version control. Use environment variables or secret management systems.

---

## Testing

After implementing these fixes, verify webhook integration:

1. **Header Forwarding**: Check gateway logs to confirm Svix headers are being forwarded
2. **Signature Verification**: Verify webhook requests pass signature validation
3. **Event Processing**: Confirm handled events are processed correctly
4. **Unhandled Events**: Verify unhandled events return 200 OK without errors
5. **Case Sensitivity**: Test with different header casing to ensure robustness

---

## Related Files

- **API Gateway Filter**: `api-gateway/src/main/java/com/demo/gateway/config/JwtAuthenticationFilter.java`
- **Webhook Controller**: `backend-service/src/main/java/com/demo/backend/controller/user/WebhookController.java`
- **Webhook Service**: `backend-service/src/main/java/com/demo/backend/service/user/WebhookService.java`

---

## Summary

These fixes ensure reliable webhook integration with Clerk:

1. ✅ Svix headers are preserved through the API Gateway
2. ✅ Webhook signatures are verified correctly using properly decoded secrets
3. ✅ Unhandled events are gracefully ignored without triggering retries
4. ✅ Header reading is case-insensitive and robust

The webhook integration now successfully synchronizes user and organization data from Clerk to our backend service.
