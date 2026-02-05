package com.wing.ecommercebackendwing.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.wing.ecommercebackendwing.dto.response.order.AddressResponse;
import com.wing.ecommercebackendwing.dto.response.user.SavedPaymentMethodResponse;

import java.time.Instant;
import java.util.List;
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
    private String avatarUrl;
    private String phoneNumber;
    private Boolean isActive;
    private Boolean emailVerified;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean twofaEnabled;
    private List<AddressResponse> addresses;
    private List<SavedPaymentMethodResponse> savedPaymentMethods;
}
