package com.wing.ecommercebackendwing.dto.response.user;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SavedPaymentMethodResponse {
    private UUID id;
    private String method;
    private String brand;
    private String last4;
    private Integer expMonth;
    private Integer expYear;
    private Boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}
