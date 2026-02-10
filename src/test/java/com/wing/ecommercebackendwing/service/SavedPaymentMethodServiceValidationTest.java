package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.request.paymentmethod.CreateSavedPaymentMethodRequest;
import com.wing.ecommercebackendwing.dto.request.paymentmethod.UpdateSavedPaymentMethodRequest;
import com.wing.ecommercebackendwing.dto.response.user.SavedPaymentMethodResponse;
import com.wing.ecommercebackendwing.exception.custom.BadRequestException;
import com.wing.ecommercebackendwing.model.entity.SavedPaymentMethod;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.model.enums.PaymentMethod;
import com.wing.ecommercebackendwing.repository.SavedPaymentMethodRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedPaymentMethodServiceValidationTest {

    @Mock
    private SavedPaymentMethodRepository savedPaymentMethodRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SavedPaymentMethodService savedPaymentMethodService;

    @Test
    void createPaymentMethod_shouldNormalizeLowercaseEnumValue() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        CreateSavedPaymentMethodRequest request = CreateSavedPaymentMethodRequest.builder()
                .method("khqr")
                .providerToken("provider-token")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(savedPaymentMethodRepository.save(any(SavedPaymentMethod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SavedPaymentMethodResponse response = savedPaymentMethodService.createPaymentMethod(userId, request);

        assertEquals(PaymentMethod.KHQR.name(), response.getMethod());
    }

    @Test
    void updatePaymentMethod_shouldRejectInvalidMonthWithoutSaving() {
        UUID userId = UUID.randomUUID();
        UUID methodId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        SavedPaymentMethod method = new SavedPaymentMethod();
        method.setId(methodId);
        method.setUser(user);

        UpdateSavedPaymentMethodRequest request = UpdateSavedPaymentMethodRequest.builder()
                .expMonth(13)
                .build();

        when(savedPaymentMethodRepository.findById(methodId)).thenReturn(Optional.of(method));

        assertThrows(BadRequestException.class,
                () -> savedPaymentMethodService.updatePaymentMethod(userId, methodId, request));

        verify(savedPaymentMethodRepository, never()).save(any(SavedPaymentMethod.class));
    }
}
