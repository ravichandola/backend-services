package com.demo.backend.repository.user;

import com.demo.backend.entity.user.Membership;
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
    
    /**
     * Find all memberships for a user with organization and role eagerly fetched
     * Used for user listing with roles
     */
    @Query("SELECT m FROM Membership m " +
           "JOIN FETCH m.organization " +
           "JOIN FETCH m.role " +
           "WHERE m.user.id = :userId")
    List<Membership> findByUserIdWithRelations(@Param("userId") Long userId);
    
    @Query("SELECT m FROM Membership m WHERE m.organization.id = :orgId")
    List<Membership> findByOrganizationId(@Param("orgId") Long orgId);
    
    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId AND m.organization.id = :orgId AND m.role.name = :roleName")
    Optional<Membership> findByUserIdAndOrganizationIdAndRoleName(
        @Param("userId") Long userId, 
        @Param("orgId") Long orgId,
        @Param("roleName") String roleName
    );
    
    /**
     * Find all ADMIN memberships for a user (across all organizations)
     * Used to check if user has admin privileges in any organization
     */
    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId AND m.role.name = 'ADMIN'")
    List<Membership> findAdminMembershipsByUserId(@Param("userId") Long userId);
    
    /**
     * Count ADMIN memberships for a user (across all organizations)
     * Used as fallback when prepared statement conflicts occur
     * More efficient than fetching all memberships
     */
    @Query("SELECT COUNT(m) FROM Membership m WHERE m.user.id = :userId AND m.role.name = 'ADMIN'")
    long countAdminMembershipsByUserId(@Param("userId") Long userId);
    
    /**
     * Count memberships for an organization
     * More efficient than fetching all memberships and calling .size()
     */
    @Query("SELECT COUNT(m) FROM Membership m WHERE m.organization.id = :orgId")
    long countByOrganizationId(@Param("orgId") Long orgId);
}
