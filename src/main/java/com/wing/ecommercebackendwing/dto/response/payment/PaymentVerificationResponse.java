package com.wing.ecommercebackendwing.dto.response.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentVerificationResponse {
    @JsonProperty("isPaid")
    private boolean isPaid;
    
    @JsonProperty("expired")
    private boolean expired;
    
    private Double paidAmount;
    private String currency;
    private String message;
}
