package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.repository.PromotionRepository;
import com.wing.ecommercebackendwing.repository.PromotionUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;

    public Object validatePromotion(String code) {
        // TODO: Validate promotion code
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public Object applyDiscount(Object applyRequest) {
        // TODO: Apply discount to order/cart
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
