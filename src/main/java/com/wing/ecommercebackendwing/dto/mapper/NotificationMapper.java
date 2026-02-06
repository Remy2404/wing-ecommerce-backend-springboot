package com.wing.ecommercebackendwing.dto.mapper;

import com.wing.ecommercebackendwing.dto.response.common.NotificationResponse;
import com.wing.ecommercebackendwing.model.entity.Notification;

public class NotificationMapper {
    public static NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUser() != null ? notification.getUser().getId() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .relatedId(notification.getRelatedId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

