package com.wing.ecommercebackendwing.dto.mapper;

import com.wing.ecommercebackendwing.dto.request.promotion.PromotionCreateRequest;
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

    public static Promotion toEntity(PromotionCreateRequest request) {
        Promotion promotion = new Promotion();
        promotion.setCode(request.getCode());
        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());
        promotion.setType(request.getType());
        promotion.setValue(request.getValue());
        promotion.setMinOrderAmount(request.getMinOrderAmount());
        promotion.setMaxDiscount(request.getMaxDiscount());
        promotion.setUsageLimit(request.getUsageLimit());
        promotion.setPerUserLimit(request.getPerUserLimit() != null ? request.getPerUserLimit() : 1);
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        promotion.setApplicableCategories(request.getApplicableCategories());
        promotion.setApplicableMerchants(request.getApplicableMerchants());
        return promotion;
    }
}
