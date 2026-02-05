package com.wing.ecommercebackendwing.dto.response.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AddressResponse {
    private UUID id;
    private UUID userId;
    private String label;
    private String fullName;
    private String phone;
    private String street;
    private String city;
    private String district;
    private String state;
    private String country;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}
