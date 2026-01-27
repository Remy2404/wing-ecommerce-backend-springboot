package com.wing.ecommercebackendwing.dto.response.promotion;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PromotionResponse {
    private UUID id;
    private String code;
    private String name;
    private String type;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscount;
    private Instant endDate;
    private Boolean isActive;
}
