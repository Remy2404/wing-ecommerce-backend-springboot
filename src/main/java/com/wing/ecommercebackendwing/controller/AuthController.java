package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.auth.*;
import com.wing.ecommercebackendwing.dto.response.auth.AuthResponse;
import com.wing.ecommercebackendwing.dto.response.auth.TwoFactorResponse;
import com.wing.ecommercebackendwing.dto.response.common.MessageResponse;
import com.wing.ecommercebackendwing.dto.response.common.ValidationErrorResponse;
import com.wing.ecommercebackendwing.security.CustomUserDetails;
import com.wing.ecommercebackendwing.security.jwt.JwtTokenProvider;
import com.wing.ecommercebackendwing.security.jwt.TokenBlacklistService;
import com.wing.ecommercebackendwing.service.EnhancedAuthService;
import com.wing.ecommercebackendwing.service.TwoFactorAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Enhanced Authentication", description = "Authentication with email verification, 2FA, and refresh tokens")
public class AuthController {

    private final EnhancedAuthService authService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${cookie.secure:true}")
    private boolean cookieSecure;
 
    @Value("${jwt.refresh-token.expiration:2592000000}")
    private long refreshTokenDurationMs;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/auth";

    private ResponseCookie createRefreshTokenCookie(String tokenValue, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, tokenValue)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user with email verification")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user (may require 2FA)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse httpResponse) {
        AuthResponse response = authService.login(request);
        
        // Set refresh token as HttpOnly cookie (not accessible via JavaScript)
        if (response.getRefreshToken() != null) {
            long maxAgeSeconds = refreshTokenDurationMs / 1000;
            ResponseCookie cookie = createRefreshTokenCookie(response.getRefreshToken(), maxAgeSeconds);
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            response.setRefreshToken(null); // Remove from response body
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/2fa")
    @Operation(summary = "Complete login with 2FA code")
    public ResponseEntity<AuthResponse> loginWith2FA(
            @RequestParam(name = "email") String email,
            @RequestParam(name = "code") int code,
            HttpServletResponse httpResponse) {
        AuthResponse response = authService.verify2FAAndLogin(email, code);
        
        // Set refresh token as HttpOnly cookie
        if (response.getRefreshToken() != null) {
            long maxAgeSeconds = refreshTokenDurationMs / 1000;
            ResponseCookie cookie = createRefreshTokenCookie(response.getRefreshToken(), maxAgeSeconds);
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            response.setRefreshToken(null);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Get new access token using refresh token from HttpOnly cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid refresh token cookie"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<AuthResponse> refreshToken(
            @CookieValue(name = "refreshToken") String refreshToken,
            HttpServletResponse httpResponse) {
        AuthResponse response = authService.refreshToken(refreshToken);
        
        // Set new refresh token as HttpOnly cookie
        if (response.getRefreshToken() != null) {
            long maxAgeSeconds = refreshTokenDurationMs / 1000;
            ResponseCookie cookie = createRefreshTokenCookie(response.getRefreshToken(), maxAgeSeconds);
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            response.setRefreshToken(null);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or invalid token",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.getToken());
        return ResponseEntity.ok(MessageResponse.builder()
                .success(true)
                .message("Email verified successfully")
                .build());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset email sent"),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok(MessageResponse.builder()
                .success(true)
                .message("Password reset email sent")
                .build());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or invalid token",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody CompletePasswordResetRequest request) {
        authService.resetPasswordWithToken(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(MessageResponse.builder()
                .success(true)
                .message("Password reset successfully")
                .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke all tokens")
    public ResponseEntity<MessageResponse> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletResponse httpResponse) {
        
        // Blacklist access token if present
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            String jti = jwtTokenProvider.getJtiFromToken(accessToken);
            if (jti != null) {
                tokenBlacklistService.blacklist(jti);
            }
        }
        
        // Revoke refresh tokens in database
        authService.logout(userDetails.getUserId());
        
        // Delete refresh token cookie
        ResponseCookie cookie = createRefreshTokenCookie("", 0);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        
        return ResponseEntity.ok(MessageResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .build());
    }

    // 2FA Endpoints
    @PostMapping("/2fa/setup")
    @Operation(summary = "Generate 2FA secret and QR code")
    public ResponseEntity<TwoFactorResponse> setup2FA(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails.getUserId();
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
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody Enable2FARequest request) {
        UUID userId = userDetails.getUserId();
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
    public ResponseEntity<MessageResponse> disable2FA(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails.getUserId();
        twoFactorAuthService.disable2FA(userId);
        
        return ResponseEntity.ok(MessageResponse.builder()
                .success(true)
                .message("2FA disabled successfully")
                .build());
    }
}
