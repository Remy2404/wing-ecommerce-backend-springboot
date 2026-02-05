package com.wing.ecommercebackendwing.dto.mapper;

import com.wing.ecommercebackendwing.dto.response.user.SavedPaymentMethodResponse;
import com.wing.ecommercebackendwing.model.entity.SavedPaymentMethod;

public class SavedPaymentMethodMapper {

    public static SavedPaymentMethodResponse toResponse(SavedPaymentMethod method) {
        return SavedPaymentMethodResponse.builder()
                .id(method.getId())
                .method(method.getMethod().name())
                .brand(method.getBrand())
                .last4(method.getLast4())
                .expMonth(method.getExpMonth())
                .expYear(method.getExpYear())
                .isDefault(method.getIsDefault())
                .createdAt(method.getCreatedAt())
                .updatedAt(method.getUpdatedAt())
                .build();
    }
}
