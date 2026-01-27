package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.UserMapper;
import com.wing.ecommercebackendwing.dto.request.user.UpdateProfileRequest;
import com.wing.ecommercebackendwing.dto.response.auth.UserResponse;
import com.wing.ecommercebackendwing.dto.response.user.UserStatsResponse;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.NotificationRepository;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import com.wing.ecommercebackendwing.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final WishlistRepository wishlistRepository;
    private final NotificationRepository notificationRepository;

    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        return UserMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest updateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        if (updateRequest.getFirstName() != null) user.setFirstName(updateRequest.getFirstName());
        if (updateRequest.getLastName() != null) user.setLastName(updateRequest.getLastName());
        if (updateRequest.getPhoneNumber() != null) user.setPhone(updateRequest.getPhoneNumber());
        
        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    public UserStatsResponse getDashboardStats(UUID userId) {
        return UserStatsResponse.builder()
                .totalOrders(orderRepository.countByUserId(userId))
                .wishlistCount(wishlistRepository.countByUserId(userId))
                .unreadNotifications(notificationRepository.countByUserIdAndIsReadFalse(userId))
                .build();
    }
}
