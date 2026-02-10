package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.exception.custom.BadRequestException;
import com.wing.ecommercebackendwing.repository.NotificationRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceValidationTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void markAsRead_shouldRejectEmptyIdsWithoutRepositoryCalls() {
        assertThrows(BadRequestException.class,
                () -> notificationService.markAsRead(UUID.randomUUID(), List.of()));

        verify(notificationRepository, never()).findAllById(org.mockito.ArgumentMatchers.any());
        verify(notificationRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }
}
