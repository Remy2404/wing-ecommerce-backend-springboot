package com.wing.ecommercebackendwing.dto.response.auth;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String token;

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
