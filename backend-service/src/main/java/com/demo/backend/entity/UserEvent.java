package com.demo.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_events", indexes = {
    @Index(name = "idx_user_events_clerk_user_id", columnList = "clerk_user_id"),
    @Index(name = "idx_user_events_event_type", columnList = "event_type"),
    @Index(name = "idx_user_events_processed_at", columnList = "processed_at"),
    @Index(name = "idx_user_events_clerk_event_id", columnList = "clerk_event_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "clerk_user_id", nullable = false)
    private String clerkUserId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", nullable = false, columnDefinition = "jsonb")
    private String eventData;
    
    @Column(name = "clerk_event_id")
    private String clerkEventId;
    
    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;
}
