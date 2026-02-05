package com.wing.ecommercebackendwing.dto.mapper;

import com.wing.ecommercebackendwing.dto.request.auth.RegisterRequest;
import com.wing.ecommercebackendwing.dto.response.auth.UserResponse;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.model.enums.UserRole;

public class UserMapper {

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .avatar(user.getAvatar())
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
