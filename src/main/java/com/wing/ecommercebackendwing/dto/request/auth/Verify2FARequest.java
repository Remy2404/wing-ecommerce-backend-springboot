package com.wing.ecommercebackendwing.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class Verify2FARequest {
    @NotBlank(message = "Temporary token is required")
    private String tempToken;
    
    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    private String otp;
}
