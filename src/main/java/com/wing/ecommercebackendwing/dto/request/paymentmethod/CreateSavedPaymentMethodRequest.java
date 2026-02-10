package com.wing.ecommercebackendwing.dto.request.paymentmethod;

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
public class CreateSavedPaymentMethodRequest {
    @NotBlank
    @Pattern(
            regexp = "(?i)^(KHQR|CASH_ON_DELIVERY)$",
            message = "method must be KHQR or CASH_ON_DELIVERY"
    )
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
