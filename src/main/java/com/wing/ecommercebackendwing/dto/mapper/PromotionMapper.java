package com.wing.ecommercebackendwing.dto.mapper;

import com.wing.ecommercebackendwing.dto.response.promotion.PromotionResponse;
import com.wing.ecommercebackendwing.model.entity.Promotion;

public class PromotionMapper {
    public static PromotionResponse toResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .name(promotion.getName())
                .type(promotion.getType())
                .value(promotion.getValue())
                .minOrderAmount(promotion.getMinOrderAmount())
                .maxDiscount(promotion.getMaxDiscount())
                .endDate(promotion.getEndDate())
                .isActive(promotion.getIsActive())
                .build();
    }
}
