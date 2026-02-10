package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.SavedPaymentMethodMapper;
import com.wing.ecommercebackendwing.dto.request.paymentmethod.CreateSavedPaymentMethodRequest;
import com.wing.ecommercebackendwing.dto.request.paymentmethod.UpdateSavedPaymentMethodRequest;
import com.wing.ecommercebackendwing.dto.response.user.SavedPaymentMethodResponse;
import com.wing.ecommercebackendwing.exception.custom.BadRequestException;
import com.wing.ecommercebackendwing.model.entity.SavedPaymentMethod;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.model.enums.PaymentMethod;
import com.wing.ecommercebackendwing.repository.SavedPaymentMethodRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedPaymentMethodService {

    private final SavedPaymentMethodRepository savedPaymentMethodRepository;
    private final UserRepository userRepository;

    public List<SavedPaymentMethodResponse> getUserPaymentMethods(UUID userId) {
        return savedPaymentMethodRepository.findByUserId(userId).stream()
                .map(SavedPaymentMethodMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SavedPaymentMethodResponse createPaymentMethod(UUID userId, CreateSavedPaymentMethodRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SavedPaymentMethod method = new SavedPaymentMethod();
        method.setUser(user);
        try {
            method.setMethod(PaymentMethod.valueOf(request.getMethod().trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported payment method");
        }
        method.setBrand(request.getBrand());
        method.setLast4(request.getLast4());
        method.setExpMonth(request.getExpMonth());
        method.setExpYear(request.getExpYear());
        method.setProviderToken(request.getProviderToken());
        method.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        method.setCreatedAt(Instant.now());
        method.setUpdatedAt(Instant.now());

        SavedPaymentMethod saved = savedPaymentMethodRepository.save(method);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            setDefaultPaymentMethod(userId, saved.getId());
        }

        return SavedPaymentMethodMapper.toResponse(saved);
    }

    @Transactional
    public SavedPaymentMethodResponse updatePaymentMethod(UUID userId, UUID id, UpdateSavedPaymentMethodRequest request) {
        SavedPaymentMethod method = savedPaymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found"));

        if (!method.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Payment method does not belong to user");
        }

        if (request.getBrand() != null) method.setBrand(request.getBrand().trim());
        if (request.getLast4() != null) method.setLast4(validateLast4(request.getLast4()));
        if (request.getExpMonth() != null) method.setExpMonth(validateExpMonth(request.getExpMonth()));
        if (request.getExpYear() != null) method.setExpYear(validateExpYear(request.getExpYear()));
        if (request.getIsDefault() != null) method.setIsDefault(request.getIsDefault());
        method.setUpdatedAt(Instant.now());

        SavedPaymentMethod saved = savedPaymentMethodRepository.save(method);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            setDefaultPaymentMethod(userId, saved.getId());
        }

        return SavedPaymentMethodMapper.toResponse(saved);
    }

    @Transactional
    public void deletePaymentMethod(UUID userId, UUID id) {
        SavedPaymentMethod method = savedPaymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found"));

        if (!method.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Payment method does not belong to user");
        }

        savedPaymentMethodRepository.delete(method);
    }

    @Transactional
    public SavedPaymentMethodResponse setDefaultPaymentMethod(UUID userId, UUID id) {
        SavedPaymentMethod method = savedPaymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found"));

        if (!method.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Payment method does not belong to user");
        }

        List<SavedPaymentMethod> methods = savedPaymentMethodRepository.findByUserId(userId);
        for (SavedPaymentMethod m : methods) {
            m.setIsDefault(m.getId().equals(id));
            m.setUpdatedAt(Instant.now());
        }
        savedPaymentMethodRepository.saveAll(methods);

        return SavedPaymentMethodMapper.toResponse(method);
    }

    private String validateLast4(String last4) {
        String value = last4.trim();
        if (!value.matches("\\d{4}")) {
            throw new BadRequestException("Invalid last4 digits");
        }
        return value;
    }

    private Integer validateExpMonth(Integer month) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Invalid expiration month");
        }
        return month;
    }

    private Integer validateExpYear(Integer year) {
        if (year < 2000 || year > 2100) {
            throw new BadRequestException("Invalid expiration year");
        }
        return year;
    }
}
