package com.wing.ecommercebackendwing.dto.response.common;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private String type;
    private UUID relatedId;
    private Boolean isRead;
    private Instant createdAt;
}

