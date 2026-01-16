package com.demo.backend.repository;

import com.demo.backend.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByClerkMembershipId(String clerkMembershipId);
    
    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId AND m.organization.id = :orgId")
    Optional<Membership> findByUserIdAndOrganizationId(@Param("userId") Long userId, @Param("orgId") Long orgId);
    
    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId")
    List<Membership> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT m FROM Membership m WHERE m.organization.id = :orgId")
    List<Membership> findByOrganizationId(@Param("orgId") Long orgId);
    
    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId AND m.organization.id = :orgId AND m.role.name = :roleName")
    Optional<Membership> findByUserIdAndOrganizationIdAndRoleName(
        @Param("userId") Long userId, 
        @Param("orgId") Long orgId,
        @Param("roleName") String roleName
    );
}
