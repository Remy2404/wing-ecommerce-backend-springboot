package com.wing.ecommercebackendwing.dto.request.paymentmethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSavedPaymentMethodRequest {
    private String brand;
    private String last4;
    private Integer expMonth;
    private Integer expYear;
    private Boolean isDefault;
}
