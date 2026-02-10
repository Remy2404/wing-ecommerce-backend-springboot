package com.wing.ecommercebackendwing.dto.request.address;

import jakarta.validation.constraints.Pattern;
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
    @Pattern(
            regexp = "^(\\+?[1-9]\\d{1,14}|0\\d{8,9})$",
            message = "Invalid phone number format"
    )
    private String phone;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    @Builder.Default
    private Boolean isDefault = false;
}
