package com.demo.backend.repository.user;

import com.demo.backend.entity.user.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByClerkOrgId(String clerkOrgId);
    boolean existsByClerkOrgId(String clerkOrgId);
}
