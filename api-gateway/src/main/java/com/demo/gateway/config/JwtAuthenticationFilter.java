package com.demo.gateway.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;

/**
 * Global filter for JWT authentication
 * Validates Clerk JWT tokens using JWKS (JSON Web Key Set)
 * 
 * Flow:
 * 1. Extract JWT from Authorization header
 * 2. Parse JWT to get key ID (kid)
 * 3. Fetch public key from Clerk JWKS endpoint
 * 4. Validate JWT signature and claims
 * 5. Extract user ID and org ID from claims
 * 6. Add X-User-Id and X-Org-Id headers for backend service
 * 7. Allow webhook endpoints without authentication
 */
@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    
    @Value("${clerk.jwks.url:https://api.clerk.dev/v1/jwks}")
    private String jwksUrl;
    
    @Value("${clerk.issuer:https://clerk.dev}")
    private String issuer;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, PublicKey> publicKeyCache = new ConcurrentHashMap<>();
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // Allow webhook endpoints without authentication
        if (path.startsWith("/api/webhooks")) {
            log.debug("Allowing webhook endpoint without authentication: {}", path);
            return chain.filter(exchange);
        }
        
        // Allow health check
        if (path.equals("/api/health")) {
            return chain.filter(exchange);
        }
        
        // Allow payment APIs without authentication (public endpoints)
        if (path.startsWith("/api/payments")) {
            log.debug("Allowing payment endpoint without authentication: {}", path);
            return chain.filter(exchange);
        }
        
        // Extract JWT from Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            return unauthorized(exchange);
        }
        
        String token = authHeader.substring(7);
        
        try {
            // Parse and validate JWT
            Claims claims = validateJwt(token);
            
            // Extract user ID and org ID from claims
            String userId = claims.getSubject(); // Clerk uses 'sub' for user ID
            String orgId = claims.get("org_id", String.class);
            
            if (userId == null) {
                log.warn("JWT missing user ID (sub claim)");
                return unauthorized(exchange);
            }
            
            // Add headers for backend service
            ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-Org-Id", orgId != null ? orgId : "")
                .build();
            
            log.debug("JWT validated. User: {}, Org: {}", userId, orgId);
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
            
        } catch (Exception e) {
            log.error("JWT validation failed", e);
            return unauthorized(exchange);
        }
    }
    
    /**
     * Validate JWT token using Clerk JWKS
     */
    private Claims validateJwt(String token) throws Exception {
        // Parse JWT header to get key ID (kid)
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        JsonNode header = objectMapper.readTree(headerJson);
        String kid = header.get("kid").asText();
        
        // Get public key from cache or fetch from JWKS
        PublicKey publicKey = getPublicKey(kid);
        
        // Build JWT parser (jjwt 0.12.x API)
        JwtParser parser = Jwts.parser()
            .verifyWith(publicKey)
            .requireIssuer(issuer)
            .build();
        
        // Parse and validate JWT
        Claims claims = parser.parseSignedClaims(token).getPayload();
        
        // Additional validation
        if (claims.getExpiration() != null && claims.getExpiration().getTime() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("JWT expired");
        }
        
        return claims;
    }
    
    /**
     * Get public key from cache or fetch from Clerk JWKS endpoint
     */
    private PublicKey getPublicKey(String kid) throws Exception {
        // Check cache first
        if (publicKeyCache.containsKey(kid)) {
            return publicKeyCache.get(kid);
        }
        
        // Fetch JWKS from Clerk
        String jwksJson = fetchJwks();
        JsonNode jwks = objectMapper.readTree(jwksJson);
        
        // Find the key with matching kid
        JsonNode keys = jwks.get("keys");
        if (keys == null || !keys.isArray()) {
            throw new RuntimeException("Invalid JWKS format");
        }
        
        for (JsonNode key : keys) {
            if (kid.equals(key.get("kid").asText())) {
                // Extract public key
                String modulus = key.get("n").asText();
                String exponent = key.get("e").asText();
                
                // Convert RSA public key from JWK format
                PublicKey publicKey = jwkToPublicKey(modulus, exponent);
                
                // Cache the key
                publicKeyCache.put(kid, publicKey);
                
                return publicKey;
            }
        }
        
        throw new RuntimeException("Key not found in JWKS: " + kid);
    }
    
    /**
     * Fetch JWKS from Clerk endpoint
     */
    private String fetchJwks() throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(jwksUrl))
            .GET()
            .build();
        
        java.net.http.HttpResponse<String> response = client.send(request, 
            java.net.http.HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch JWKS: " + response.statusCode());
        }
        
        return response.body();
    }
    
    /**
     * Convert JWK (JSON Web Key) to Java PublicKey
     * This is a simplified version - in production, use a library like jose4j or nimbus-jose-jwt
     */
    private PublicKey jwkToPublicKey(String modulus, String exponent) throws Exception {
        // Decode base64url-encoded values
        byte[] nBytes = Base64.getUrlDecoder().decode(modulus);
        byte[] eBytes = Base64.getUrlDecoder().decode(exponent);
        
        // Convert to BigInteger
        java.math.BigInteger n = new java.math.BigInteger(1, nBytes);
        java.math.BigInteger e = new java.math.BigInteger(1, eBytes);
        
        // Create RSA public key
        java.security.spec.RSAPublicKeySpec spec = new java.security.spec.RSAPublicKeySpec(n, e);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }
    
    /**
     * Return 401 Unauthorized response
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = "{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing JWT token\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }
    
    @Override
    public int getOrder() {
        return -100; // High priority - run early in the filter chain
    }
}
