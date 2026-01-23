package com.demo.backend.entity.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "memberships", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "organization_id"}),
    indexes = {
        @Index(name = "idx_memberships_user_id", columnList = "user_id"),
        @Index(name = "idx_memberships_organization_id", columnList = "organization_id"),
        @Index(name = "idx_memberships_role_id", columnList = "role_id"),
        @Index(name = "idx_memberships_clerk_membership_id", columnList = "clerk_membership_id"),
        @Index(name = "idx_memberships_user_org", columnList = "user_id, organization_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membership {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    @Column(name = "clerk_membership_id", nullable = false, unique = true)
    private String clerkMembershipId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
