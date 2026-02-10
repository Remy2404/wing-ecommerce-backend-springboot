package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.UserMapper;
import com.wing.ecommercebackendwing.dto.request.user.UpdateProfileRequest;
import com.wing.ecommercebackendwing.dto.response.auth.UserResponse;
import com.wing.ecommercebackendwing.dto.response.user.UserStatsResponse;
import com.wing.ecommercebackendwing.exception.custom.BadRequestException;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.NotificationRepository;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import com.wing.ecommercebackendwing.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final WishlistRepository wishlistRepository;
    private final NotificationRepository notificationRepository;
    private final PhoneNumberService phoneNumberService;
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;
    @Value("${app.public-base-url:http://localhost:8081}")
    private String publicBaseUrl;

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
        if (updateRequest.getPhoneNumber() != null) {
            user.setPhone(phoneNumberService.normalizeToE164(updateRequest.getPhoneNumber(), null));
        }
        
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

    @Transactional
    public UserResponse updateAvatar(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Avatar file is required");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BadRequestException("Avatar file must be 2MB or smaller");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed for avatar upload");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        String extension = ".jpg";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        } else if ("image/png".equals(contentType)) {
            extension = ".png";
        } else if ("image/webp".equals(contentType)) {
            extension = ".webp";
        }

        String fileName = userId + "-" + Instant.now().toEpochMilli() + extension;
        Path avatarDir = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
        Path target = avatarDir.resolve(fileName);

        try {
            Files.createDirectories(avatarDir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to store avatar image", exception);
        }

        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        user.setAvatar(base + "/uploads/avatars/" + fileName);
        userRepository.save(user);
        return UserMapper.toResponse(user);
    }
}
