package com.demo.backend.repository;

import com.demo.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByClerkUserId(String clerkUserId);
    Optional<User> findByEmail(String email);
    boolean existsByClerkUserId(String clerkUserId);
}
