package com.wing.ecommercebackendwing.dto.response.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
    @Builder.Default
    private boolean success = false;
    private String error;
    private String code;
}
