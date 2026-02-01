package com.wing.ecommercebackendwing.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Google OAuth login request")
public class GoogleLoginRequest {

    @NotBlank(message = "ID token is required")
    @Schema(
        description = "Google ID token obtained from Google Sign-In",
        example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjU5MmZjZjFjM2JhNDhmNzNhMDg5YzgwYTE0MTlkMWY4M2M4NGYxYzciLCJ0eXAiOiJKV1QifQ..."
    )
    private String idToken;
}
