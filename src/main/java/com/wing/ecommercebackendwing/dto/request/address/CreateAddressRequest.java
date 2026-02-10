package com.wing.ecommercebackendwing.dto.request.address;

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
public class CreateAddressRequest {
    @NotBlank
    private String label;

    @NotBlank
    private String fullName;

    @NotBlank
    @Pattern(
            regexp = "^(\\+?[1-9]\\d{1,14}|0\\d{8,9})$",
            message = "Invalid phone number format"
    )
    private String phone;

    @NotBlank
    private String street;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @NotBlank
    private String postalCode;

    @NotBlank
    private String country;

    @Builder.Default
    private Boolean isDefault = false;
}
