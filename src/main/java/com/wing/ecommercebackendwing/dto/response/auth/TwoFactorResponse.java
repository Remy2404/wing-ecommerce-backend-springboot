package com.wing.ecommercebackendwing.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorResponse {
    private String secret;
    private String qrCodeUrl;
    private String message;
}
