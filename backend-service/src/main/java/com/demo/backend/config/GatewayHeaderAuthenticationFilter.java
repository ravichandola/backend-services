package com.demo.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Authentication filter that trusts the API Gateway
 * 
 * This filter checks for X-User-Id header added by the API Gateway.
 * If present, it creates an authenticated SecurityContext.
 * 
 * IMPORTANT: This service trusts the API Gateway. The gateway validates JWT tokens.
 * This filter only checks for the presence of gateway headers.
 */
@Component
@Slf4j
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // Check for X-User-Id header (added by API Gateway after JWT validation)
        String userId = request.getHeader("X-User-Id");
        String orgId = request.getHeader("X-Org-Id");
        
        if (userId != null && !userId.isEmpty()) {
            // Gateway has validated JWT and added user context
            // Create authenticated authentication
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userId, // principal
                null,   // credentials (not needed)
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("✅ Authenticated request from gateway. User: {}, Org: {}", userId, orgId);
        } else {
            // No X-User-Id header - request did not come through gateway with valid JWT
            // SecurityContext will remain unauthenticated
            // Spring Security will handle authorization based on SecurityConfig
            log.warn("⚠️ No X-User-Id header found - request not authenticated by gateway. Path: {}", request.getRequestURI());
        }
        
        filterChain.doFilter(request, response);
    }
}
