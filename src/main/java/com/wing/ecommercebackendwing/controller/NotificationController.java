package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.mapper.NotificationMapper;
import com.wing.ecommercebackendwing.dto.response.common.NotificationResponse;
import com.wing.ecommercebackendwing.model.entity.Notification;
import com.wing.ecommercebackendwing.security.CustomUserDetails;
import com.wing.ecommercebackendwing.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification management APIs")
@SecurityRequirement(name = "Bearer Authentication")
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get current user's unread notifications")
    public ResponseEntity<List<NotificationResponse>> getNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Notification> notifications = notificationService.getUserNotifications(userDetails.getUserId());
        return ResponseEntity.ok(
                notifications.stream().map(NotificationMapper::toResponse).collect(Collectors.toList())
        );
    }

    @PostMapping("/read")
    @Operation(summary = "Mark notifications as read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @NotEmpty(message = "notificationIds must not be empty")
            List<@NotNull(message = "notificationId must not be null") UUID> notificationIds) {
        notificationService.markAsRead(userDetails.getUserId(), notificationIds);
        return ResponseEntity.ok().build();
    }
}
