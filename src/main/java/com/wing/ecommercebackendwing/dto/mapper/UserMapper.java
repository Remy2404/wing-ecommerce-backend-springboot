package com.wing.ecommercebackendwing.dto.mapper;

import com.wing.ecommercebackendwing.dto.request.auth.RegisterRequest;
import com.wing.ecommercebackendwing.dto.response.auth.UserResponse;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.model.enums.UserRole;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {

    public static UserResponse toResponse(User user) {
        List<com.wing.ecommercebackendwing.dto.response.order.AddressResponse> addresses =
                user.getAddresses() == null ? Collections.emptyList() :
                        user.getAddresses().stream()
                                .map(AddressMapper::toResponse)
                                .collect(Collectors.toList());

        List<com.wing.ecommercebackendwing.dto.response.user.SavedPaymentMethodResponse> savedPaymentMethods =
                user.getSavedPaymentMethods() == null ? Collections.emptyList() :
                        user.getSavedPaymentMethods().stream()
                                .map(SavedPaymentMethodMapper::toResponse)
                                .collect(Collectors.toList());

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatar())
                .phoneNumber(user.getPhone())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .twofaEnabled(user.getTwofaEnabled())
                .addresses(addresses)
                .savedPaymentMethods(savedPaymentMethods)
                .build();
    }

    public static User toEntity(RegisterRequest request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(UserRole.CUSTOMER);
        user.setIsActive(true);
        return user;
    }
}
