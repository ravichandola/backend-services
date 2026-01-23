package com.demo.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless API
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow webhook endpoint without authentication (uses signature verification)
                .requestMatchers("/api/webhooks/**").permitAll()
                // Health check is public
                .requestMatchers("/api/health").permitAll()
                // Payment APIs are public (can be called from frontend without JWT)
                .requestMatchers("/api/payments/**").permitAll()
                // Migration endpoints (for development/admin use)
                .requestMatchers("/api/migrations/**").permitAll()
                // Test endpoints (dev only - check in controller)
                .requestMatchers("/api/test/**").permitAll()
                // All other endpoints require authentication (trust gateway)
                .anyRequest().authenticated()
            )
            // Add filter to trust gateway headers
            .addFilterBefore(gatewayHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable());
        
        return http.build();
    }
}
