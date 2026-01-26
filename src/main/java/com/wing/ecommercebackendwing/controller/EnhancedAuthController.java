package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.auth.*;
import com.wing.ecommercebackendwing.dto.response.auth.AuthResponse;
import com.wing.ecommercebackendwing.dto.response.auth.TwoFactorResponse;
import com.wing.ecommercebackendwing.dto.response.common.MessageResponse;
import com.wing.ecommercebackendwing.service.EnhancedAuthService;
import com.wing.ecommercebackendwing.service.TwoFactorAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Enhanced Authentication", description = "Authentication with email verification, 2FA, and refresh tokens")
public class EnhancedAuthController {

    private final EnhancedAuthService authService;
    private final TwoFactorAuthService twoFactorAuthService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user with email verification")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user (may require 2FA)")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/2fa")
    @Operation(summary = "Complete login with 2FA code")
    public ResponseEntity<AuthResponse> loginWith2FA(
            @RequestParam String email,
            @RequestParam int code) {
        AuthResponse response = authService.verify2FAAndLogin(email, code);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Get new access token using refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address")
    public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.getToken());
        return ResponseEntity.ok(MessageResponse.builder()
                .success(true)
                .message("Email verified successfully")
                .build());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok(MessageResponse.builder()
                .success(true)
                .message("Password reset email sent")
                .build());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody CompletePasswordResetRequest request) {
        authService.resetPasswordWithToken(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(MessageResponse.builder()
                .success(true)
                .message("Password reset successfully")
                .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh tokens")
    public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal UserDetails userDetails) {
        // Extract user ID from principal
        // This is a simplified version - you may need to adjust based on your UserDetails implementation
        authService.logout(UUID.randomUUID()); // TODO: Get actual user ID from principal
        return ResponseEntity.ok(MessageResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .build());
    }

    // 2FA Endpoints
    @PostMapping("/2fa/setup")
    @Operation(summary = "Generate 2FA secret and QR code")
    public ResponseEntity<TwoFactorResponse> setup2FA(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.randomUUID(); // TODO: Get from principal
        String secret = twoFactorAuthService.generateSecret(userId);
        String qrCodeUrl = twoFactorAuthService.generateQRCodeUrl(secret, userDetails.getUsername());
        
        return ResponseEntity.ok(TwoFactorResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .message("Scan QR code with authenticator app and verify with code")
                .build());
    }

    @PostMapping("/2fa/enable")
    @Operation(summary = "Enable 2FA after verifying code")
    public ResponseEntity<MessageResponse> enable2FA(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody Enable2FARequest request) {
        UUID userId = UUID.randomUUID(); // TODO: Get from principal
        boolean enabled = twoFactorAuthService.enable2FA(userId, Integer.parseInt(request.getCode()));
        
        if (enabled) {
            return ResponseEntity.ok(MessageResponse.builder()
                    .success(true)
                    .message("2FA enabled successfully")
                    .build());
        } else {
            return ResponseEntity.badRequest().body(MessageResponse.builder()
                    .success(false)
                    .message("Invalid 2FA code")
                    .build());
        }
    }

    @PostMapping("/2fa/disable")
    @Operation(summary = "Disable 2FA")
    public ResponseEntity<MessageResponse> disable2FA(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.randomUUID(); // TODO: Get from principal
        twoFactorAuthService.disable2FA(userId);
        
        return ResponseEntity.ok(MessageResponse.builder()
                .success(true)
                .message("2FA disabled successfully")
                .build());
    }
}
