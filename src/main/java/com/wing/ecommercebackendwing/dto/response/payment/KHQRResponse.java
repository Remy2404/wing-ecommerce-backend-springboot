package com.wing.ecommercebackendwing.dto.response.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KHQRResponse {
    private String qrData;
    private String md5;
    private String orderNumber;
    private String amount;
}
