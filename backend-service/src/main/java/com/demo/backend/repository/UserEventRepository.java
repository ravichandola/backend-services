package com.demo.backend.repository;

import com.demo.backend.entity.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserEventRepository extends JpaRepository<UserEvent, Long> {
    List<UserEvent> findByClerkUserIdOrderByProcessedAtDesc(String clerkUserId);
    boolean existsByClerkEventId(String clerkEventId);
}
