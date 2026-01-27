package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.UserMapper;
import com.wing.ecommercebackendwing.dto.response.auth.UserResponse;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.NotificationRepository;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import com.wing.ecommercebackendwing.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
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
    public UserResponse updateProfile(UUID userId, Object updateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        // Note: updateRequest should be a proper DTO with validation
        // For now, this is a placeholder that needs a proper UpdateProfileRequest DTO
        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    public Object getDashboardStats(UUID userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", orderRepository.countByUserId(userId));
        stats.put("wishlistCount", wishlistRepository.countByUserId(userId));
        stats.put("unreadNotifications", notificationRepository.countByUserIdAndIsReadFalse(userId));
        return stats;
    }
}
