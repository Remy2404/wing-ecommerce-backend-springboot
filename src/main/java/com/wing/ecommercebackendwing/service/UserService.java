package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.response.auth.UserResponse;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getUserById(UUID userId) {
        // TODO: Implement get user by ID
        // Find user or throw exception
        // Map to UserResponse
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, Object updateRequest) {
        // TODO: Implement profile update
        // Find user
        // Update fields
        // Save and return response
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Object getDashboardStats(UUID userId) {
        // TODO: Implement dashboard stats
        // Aggregate user stats
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
