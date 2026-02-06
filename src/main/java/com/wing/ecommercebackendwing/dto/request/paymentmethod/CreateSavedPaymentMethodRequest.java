package com.wing.ecommercebackendwing.dto.request.paymentmethod;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSavedPaymentMethodRequest {
    @NotBlank
    private String method;

    private String brand;

    private String last4;

    private Integer expMonth;

    private Integer expYear;

    @NotBlank
    private String providerToken;

    @Builder.Default
    private Boolean isDefault = false;
}
