package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.request.common.NotificationRequest;
import com.wing.ecommercebackendwing.model.entity.Notification;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.NotificationRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void sendNotification(UUID userId, NotificationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setType(request.getType());
        notification.setRelatedId(request.getRelatedId());
        notification.setCreatedAt(Instant.now());
        notification.setIsRead(false);

        notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID userId, List<UUID> notificationIds) {
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        notifications.stream()
                .filter(n -> n.getUser().getId().equals(userId))
                .forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }
}
