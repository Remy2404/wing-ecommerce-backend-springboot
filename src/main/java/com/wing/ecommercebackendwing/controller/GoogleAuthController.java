package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.auth.GoogleLoginRequest;
import com.wing.ecommercebackendwing.dto.response.auth.AuthResponse;
import com.wing.ecommercebackendwing.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
@Tag(name = "Google OAuth Authentication", description = "Authentication using Google Sign-In")
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    @Value("${jwt.refresh.cookie.name:refreshToken}")
    private String refreshTokenCookieName;

    @Value("${jwt.refresh.cookie.max-age:604800}")
    private int refreshTokenMaxAge;

    @Value("${jwt.refresh.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${jwt.refresh.cookie.http-only:true}")
    private boolean cookieHttpOnly;

    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api";

    private String resolveSameSitePolicy() {
        // SameSite=None must be paired with Secure. For local HTTP development, use Lax.
        return cookieSecure ? "None" : "Lax";
    }

    private ResponseCookie createRefreshTokenCookie(String tokenValue) {
        return ResponseCookie.from(refreshTokenCookieName, tokenValue)
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .sameSite(resolveSameSitePolicy())
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(refreshTokenMaxAge)
                .build();
    }

    @Operation(
        summary = "Login with Google",
        description = "Authenticate using a Google ID token obtained from Google Sign-In. " +
                     "The frontend should use the Google Sign-In SDK to obtain the ID token, " +
                     "then send it to this endpoint for verification. " +
                     "Returns JWT access and refresh tokens."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully authenticated with Google",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "Successful Google Login"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - Missing or invalid ID token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Token expired or invalid",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during authentication"
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse httpResponse) {

        AuthResponse response = googleAuthService.authenticateWithGoogle(request.getIdToken());

        // Set refresh token as HttpOnly cookie
        if (response.getRefreshToken() != null) {
            ResponseCookie refreshCookie = createRefreshTokenCookie(response.getRefreshToken());
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }

        return ResponseEntity.ok(response);
    }
}
