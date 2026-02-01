package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.UserMapper;
import com.wing.ecommercebackendwing.dto.request.auth.*;
import com.wing.ecommercebackendwing.dto.response.auth.AuthResponse;
import com.wing.ecommercebackendwing.exception.custom.TokenRefreshException;
import com.wing.ecommercebackendwing.model.entity.RefreshToken;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.UserRepository;
import com.wing.ecommercebackendwing.security.jwt.JwtTokenProvider;
import com.wing.ecommercebackendwing.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;
    private final TwoFactorAuthService twoFactorAuthService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate email doesn't exist
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Validate password strength
        PasswordValidator.ValidationResult validation = PasswordValidator.validate(request.getPassword());
        if (!validation.isValid()) {
            throw new RuntimeException(validation.getMessage());
        }

        // Create user
        User user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider(com.wing.ecommercebackendwing.model.enums.AuthProvider.LOCAL);
        user.setEmailVerified(false);
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        user.setEmailVerificationSentAt(Instant.now());
        
        User savedUser = userRepository.save(user);

        // Send verification email
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getEmailVerificationToken());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
        }

        // Generate tokens (user can use the app but with limited access until verified)
        String accessToken = jwtTokenProvider.generateToken(createAuthentication(savedUser));

        return AuthResponse.builder()
                .token(accessToken)
                .user(buildUserSummary(savedUser))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Check if account is locked
        if (loginAttemptService.isBlocked(request.getEmail())) {
            throw new RuntimeException("Account is temporarily locked due to multiple failed login attempts");
        }

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(request.getEmail());
            throw new RuntimeException("Invalid email or password");
        }

        // Check if 2FA is enabled
        if (Boolean.TRUE.equals(user.getTwofaEnabled())) {
            // Partial response - 2FA required, no token yet
            log.info("2FA required for user: {}", request.getEmail());
            throw new RuntimeException("2FA_REQUIRED");
        }

        // Successful login
        return completeLogin(user);
    }

    @Transactional
    public AuthResponse verify2FAAndLogin(String email, int code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getTwofaEnabled())) {
            throw new RuntimeException("2FA is not enabled for this account");
        }

        boolean isValid = twoFactorAuthService.verify2FACode(user.getTwofaSecret(), code);
        if (!isValid) {
            throw new RuntimeException("Invalid 2FA code");
        }

        return completeLogin(user);
    }

    private AuthResponse completeLogin(User user) {
        loginAttemptService.loginSucceeded(user.getEmail());

        // Create authentication
        Authentication authentication = createAuthentication(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(buildUserSummary(user))
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new TokenRefreshException(refreshTokenStr, "Refresh token not found"));

        refreshToken = refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        // Revoke the used refresh token and create a new one
        refreshTokenService.revokeToken(refreshTokenStr);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        String newAccessToken = jwtTokenProvider.generateToken(createAuthentication(user));

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .user(buildUserSummary(user))
                .build();
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        // Check if token is expired (24 hours)
        if (user.getEmailVerificationSentAt().isBefore(Instant.now().minus(24, ChronoUnit.HOURS))) {
            throw new RuntimeException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationSentAt(null);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(email, resetToken);
        log.info("Password reset initiated for: {}", email);
    }

    @Transactional
    public void resetPasswordWithToken(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        if (user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        // Validate new password
        PasswordValidator.ValidationResult validation = PasswordValidator.validate(newPassword);
        if (!validation.isValid()) {
            throw new RuntimeException(validation.getMessage());
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Password reset completed for user: {}", user.getEmail());
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenService.revokeAllUserTokens(userId);
        log.info("User logged out: {}", userId);
    }

    private Authentication createAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_" + user.getRole().name())
        );
    }

    private AuthResponse.UserSummary buildUserSummary(User user) {
        return AuthResponse.UserSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole().name())
                .build();
    }
}
