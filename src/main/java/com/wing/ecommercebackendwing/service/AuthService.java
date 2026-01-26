package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.request.auth.LoginRequest;
import com.wing.ecommercebackendwing.dto.request.auth.RegisterRequest;
import com.wing.ecommercebackendwing.dto.request.auth.ResetPasswordRequest;
import com.wing.ecommercebackendwing.dto.response.auth.AuthResponse;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // private final JwtUtil jwtUtil; // assume exists

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // TODO: Implement registration logic
        // Check if email exists
        // Create user with encoded password
        // Generate JWT token
        // Return AuthResponse
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public AuthResponse login(LoginRequest request) {
        // TODO: Implement login logic
        // Find user by email
        // Verify password
        // Generate JWT token
        // Return AuthResponse
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void resetPassword(ResetPasswordRequest request) {
        // TODO: Implement password reset logic
        // Find user by email
        // Generate reset token
        // Send email
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
