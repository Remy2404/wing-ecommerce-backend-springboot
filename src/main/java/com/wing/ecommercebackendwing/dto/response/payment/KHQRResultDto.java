package com.wing.ecommercebackendwing.dto.response.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KHQRResultDto {
    private String qrString;
    private String md5;
}
