package com.wing.ecommercebackendwing.dto.request.address;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAddressRequest {
    private String label;
    private String fullName;
    private String phone;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    @Builder.Default
    private Boolean isDefault = false;
}
