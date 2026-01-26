package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void sendNotification(UUID userId, Object sendRequest) {
        // TODO: Send notification to user
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<Object> getUserNotifications(UUID userId) {
        // TODO: Get user's notifications
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public void markAsRead(UUID userId, List<UUID> notificationIds) {
        // TODO: Mark notifications as read
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
