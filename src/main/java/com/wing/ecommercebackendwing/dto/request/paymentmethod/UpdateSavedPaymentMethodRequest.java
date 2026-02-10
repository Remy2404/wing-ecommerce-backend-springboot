package com.wing.ecommercebackendwing.dto.request.paymentmethod;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^\\d{4}$", message = "last4 must contain exactly 4 digits")
    private String last4;
    @Min(value = 1, message = "expMonth must be between 1 and 12")
    @Max(value = 12, message = "expMonth must be between 1 and 12")
    private Integer expMonth;
    @Min(value = 2000, message = "expYear must be between 2000 and 2100")
    @Max(value = 2100, message = "expYear must be between 2000 and 2100")
    private Integer expYear;
    private Boolean isDefault;
}
