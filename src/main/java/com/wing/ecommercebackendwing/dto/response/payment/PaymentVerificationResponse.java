package com.wing.ecommercebackendwing.dto.response.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentVerificationResponse {
    private boolean isPaid;
    private boolean expired;
    private Double paidAmount;
    private String currency;
    private String message;
}
