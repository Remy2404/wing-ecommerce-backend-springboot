package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.UserMapper;
import com.wing.ecommercebackendwing.dto.request.auth.*;
import com.wing.ecommercebackendwing.dto.response.auth.AuthResponse;
import com.wing.ecommercebackendwing.exception.custom.BadRequestException;
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
        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new com.wing.ecommercebackendwing.exception.custom.BadRequestException("Passwords do not match");
        }

        // Validate email doesn't exist
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new com.wing.ecommercebackendwing.exception.custom.BadRequestException("Email already registered");
        }

        // Validate phone doesn't exist
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new com.wing.ecommercebackendwing.exception.custom.BadRequestException("Phone number already registered");
        }

        // Validate password strength
        PasswordValidator.ValidationResult validation = PasswordValidator.validate(request.getPassword());
        if (!validation.isValid()) {
            throw new com.wing.ecommercebackendwing.exception.custom.BadRequestException(validation.getMessage());
        }

        // Create user
        User user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider(com.wing.ecommercebackendwing.model.enums.AuthProvider.LOCAL);
        user.setEmailVerified(false);
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        user.setEmailVerificationSentAt(Instant.now());
        
        // Explicitly set timestamps (since JPA auditing is not enabled)
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        
        User savedUser = userRepository.save(user);

        // Registration is only successful when verification email is sent.
        // If email delivery fails, throw and rollback the transaction.
        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getEmailVerificationToken());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", savedUser.getEmail(), e);
            throw new BadRequestException("Registration failed: could not send verification email. Please try again.");
        }

        return AuthResponse.builder()
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

        // Check email verification
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeException("EMAIL_NOT_VERIFIED");
        }

        // Check if 2FA is enabled
        if (Boolean.TRUE.equals(user.getTwofaEnabled())) {
            log.info("2FA required for user: {}", request.getEmail());
            
            // Generate temporary token (userId-based, 5 minutes)
            String tempToken = jwtTokenProvider.generateTempToken(user.getId());
            
            // Return ONLY tempToken, no access/refresh tokens
            return AuthResponse.builder()
                    .tempToken(tempToken)
                    .build();
        }

        // Successful login
        return completeLogin(user);
    }

    @Transactional
    public AuthResponse verify2FAAndLogin(UUID userId, int code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getTwofaEnabled())) {
            throw new RuntimeException("2FA is not enabled for this account");
        }

        boolean isValid = twoFactorAuthService.verify2FACode(user.getTwofaSecret(), code);
        if (!isValid) {
            throw new RuntimeException("INVALID_OTP");
        }

        return completeLogin(user);
    }

    private AuthResponse completeLogin(User user) {
        loginAttemptService.loginSucceeded(user.getEmail());

        // Create authentication
        Authentication authentication = createAuthentication(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // Update last login (optional, but good practice)
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(buildUserSummary(user))
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenService.findByTokenIncludingRevoked(refreshTokenStr)
                .orElseThrow(() -> new TokenRefreshException(refreshTokenStr, "Refresh token not found"));

        if (Boolean.TRUE.equals(refreshToken.getRevoked())
                && !refreshTokenService.isRecentlyRevoked(refreshToken)) {
            throw new TokenRefreshException(refreshTokenStr, "Refresh token revoked. Please login again");
        }

        refreshToken = refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        // Revoke the used refresh token and create a new one
        refreshTokenService.revokeToken(refreshTokenStr);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        String newAccessToken = jwtTokenProvider.generateToken(user);

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
    public AuthResponse verifyEmailAndLogin(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (user.getEmailVerificationSentAt().isBefore(Instant.now().minus(24, ChronoUnit.HOURS))) {
            throw new RuntimeException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationSentAt(null);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
        return completeLogin(user);
    }

    @Transactional
    public void resendVerificationByToken(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeException("Email is already verified");
        }

        user.setEmailVerificationToken(UUID.randomUUID().toString());
        user.setEmailVerificationSentAt(Instant.now());
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getEmailVerificationToken());
        log.info("Verification email re-sent to: {}", user.getEmail());
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }

        User user = userRepository.findByEmail(email.trim())
                .orElse(null);

        // Security: don't leak whether an email exists
        if (user == null) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(email, resetToken);
        log.info("Password reset initiated for: {}", email);
    }

    @Transactional
    public void resetPasswordWithToken(String token, String newPassword) {
        String normalizedToken = normalizeResetToken(token);
        if (normalizedToken == null || normalizedToken.isEmpty()) {
            throw new BadRequestException("Token is required");
        }

        User user = userRepository.findByPasswordResetToken(normalizedToken)
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));

        if (user.getPasswordResetTokenExpiry() == null || user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            throw new BadRequestException("Reset token has expired");
        }

        // Validate new password
        PasswordValidator.ValidationResult validation = PasswordValidator.validate(newPassword);
        if (!validation.isValid()) {
            throw new BadRequestException(validation.getMessage());
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        // Security: Revoke all refresh tokens after password reset
        refreshTokenService.revokeAllUserTokens(user.getId());

        log.info("Password reset completed and all sessions revoked for user: {}", user.getEmail());
    }

    private String normalizeResetToken(String token) {
        if (token == null) return null;

        String t = token.trim();
        if (t.isEmpty()) return "";

        // Sometimes clients accidentally send the full URL or query segment.
        // Accept either `.../reset-password?token=...` or `token=...`.
        int tokenIndex = t.indexOf("token=");
        if (tokenIndex >= 0) {
            t = t.substring(tokenIndex + "token=".length());
            int ampIndex = t.indexOf('&');
            if (ampIndex >= 0) t = t.substring(0, ampIndex);
            t = t.trim();
        }

        // Strip accidental wrapping quotes
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1).trim();
        }

        return t;
    }

    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Invalid current password");
        }

        // Validate new password strength
        PasswordValidator.ValidationResult validation = PasswordValidator.validate(newPassword);
        if (!validation.isValid()) {
            throw new RuntimeException(validation.getMessage());
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Security: Revoke all refresh tokens
        refreshTokenService.revokeAllUserTokens(userId);

        log.info("Password changed and all sessions revoked for user: {}", user.getEmail());
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
                .avatar(user.getAvatar())
                .build();
    }
}
