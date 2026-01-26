package com.wing.ecommercebackendwing.dto.response.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AddressResponse {
    private UUID id;
    private UUID userId;
    private String label;
    private String street;
    private String city;
    private String district;
    private String province;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isDefault;
}
