package com.wing.ecommercebackendwing.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddressRequest {
    @NotBlank
    private String fullName;

    @NotBlank
    private String street;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @NotBlank
    private String zipCode;

    @NotBlank
    private String country;

    @NotBlank
    @Pattern(
            regexp = "^(\\+?[1-9]\\d{1,14}|0\\d{8,9})$",
            message = "Invalid phone number format"
    )
    private String phone;
}
