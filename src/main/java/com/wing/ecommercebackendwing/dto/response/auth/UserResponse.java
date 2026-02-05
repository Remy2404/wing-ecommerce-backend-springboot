package com.wing.ecommercebackendwing.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String avatar;
    private boolean twofaEnabled;
}
