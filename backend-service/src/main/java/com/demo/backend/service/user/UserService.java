package com.demo.backend.service.user;

import com.demo.backend.dto.user.UserResponse;
import com.demo.backend.entity.user.User;
import com.demo.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for user-related business logic
 * Handles user data retrieval with proper authorization checks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    
    /**
     * Get all users with pagination
     * Requires: User must be ADMIN in at least one organization
     * 
     * @param clerkUserId The Clerk user ID from gateway header (X-User-Id)
     * @param pageable Pagination parameters (page, size, sort)
     * @return Paginated list of users
     * @throws org.springframework.security.access.AccessDeniedException if user is not admin
     */
    public Page<UserResponse> getAllUsers(String clerkUserId, Pageable pageable) {
        log.info("Getting all users - requested by user: {}", clerkUserId);
        
        // Check authorization: User must be ADMIN in at least one organization
        if (!authorizationService.isAdminInAnyOrganization(clerkUserId)) {
            log.warn("User {} attempted to fetch all users without ADMIN role", clerkUserId);
            throw new org.springframework.security.access.AccessDeniedException(
                "Forbidden: ADMIN role required to fetch all users"
            );
        }
        
        // Apply default sorting if not specified (newest first)
        Pageable sortedPageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            sortedPageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }
        
        // Fetch users from repository with pagination
        Page<User> usersPage = userRepository.findAll(sortedPageable);
        
        log.info("Found {} users (page {} of {})", 
            usersPage.getTotalElements(), 
            usersPage.getNumber() + 1, 
            usersPage.getTotalPages());
        
        // Map entities to DTOs
        Page<UserResponse> responsePage = usersPage.map(this::mapToResponse);
        
        return responsePage;
    }
    
    /**
     * Map User entity to UserResponse DTO
     * Excludes sensitive fields like clerkUserId
     */
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .imageUrl(user.getImageUrl())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
