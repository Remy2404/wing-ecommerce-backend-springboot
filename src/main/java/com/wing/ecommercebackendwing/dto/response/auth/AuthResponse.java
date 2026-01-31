package com.wing.ecommercebackendwing.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String token;
    
    @JsonIgnore // Refresh token is sent via HttpOnly cookie, not in response body
    private String refreshToken;

    private UserSummary user;

    @Data
    @Builder
    public static class UserSummary {
        private UUID id;
        private String email;
        private String name;
        private String role;
    }
}
