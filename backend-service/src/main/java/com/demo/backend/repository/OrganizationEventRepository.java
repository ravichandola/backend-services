package com.demo.backend.repository;

import com.demo.backend.entity.OrganizationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationEventRepository extends JpaRepository<OrganizationEvent, Long> {
    List<OrganizationEvent> findByClerkOrgIdOrderByProcessedAtDesc(String clerkOrgId);
    boolean existsByClerkEventId(String clerkEventId);
}
