package com.demo.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for manually triggering Flyway migrations
 */
@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@Slf4j
public class MigrationController {
    
    private final Flyway flyway;
    
    @PostMapping("/migrate")
    public ResponseEntity<Map<String, Object>> runMigrations() {
        try {
            log.info("Starting Flyway migrations...");
            
            // Run migrations
            MigrateResult result = flyway.migrate();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("migrationsExecuted", result.migrationsExecuted);
            response.put("currentVersion", flyway.info().current() != null 
                ? flyway.info().current().getVersion().toString() 
                : "baseline");
            response.put("message", "Migrations completed successfully");
            
            log.info("Flyway migrations completed. Executed: {}", result.migrationsExecuted);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error running migrations", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @PostMapping("/info")
    public ResponseEntity<Map<String, Object>> getMigrationInfo() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("currentVersion", flyway.info().current() != null 
                ? flyway.info().current().getVersion().toString() 
                : "baseline");
            response.put("pendingMigrations", flyway.info().pending().length);
            response.put("allMigrations", flyway.info().all().length);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting migration info", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
