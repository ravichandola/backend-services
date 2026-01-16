package com.demo.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_organizations_clerk_org_id", columnList = "clerk_org_id"),
    @Index(name = "idx_organizations_slug", columnList = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "clerk_org_id", nullable = false, unique = true)
    private String clerkOrgId;
    
    @Column(nullable = false)
    private String name;
    
    private String slug;
    
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
