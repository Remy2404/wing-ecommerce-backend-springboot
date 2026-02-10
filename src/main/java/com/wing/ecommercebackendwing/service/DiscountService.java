package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.model.entity.Promotion;
import com.wing.ecommercebackendwing.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final PromotionRepository promotionRepository;

    public BigDecimal calculateDiscount(BigDecimal subtotal, String couponCode) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0 || couponCode == null) {
            return BigDecimal.ZERO;
        }

        String code = couponCode.toUpperCase().trim();

        Optional<Promotion> promotionOpt = promotionRepository.findByCodeIgnoreCase(code);
        if (promotionOpt.isPresent()) {
            Promotion promotion = promotionOpt.get();
            Instant now = Instant.now();

            if (!Boolean.TRUE.equals(promotion.getIsActive())) {
                return BigDecimal.ZERO;
            }
            if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) {
                return BigDecimal.ZERO;
            }
            if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
                return BigDecimal.ZERO;
            }
            if (promotion.getMinOrderAmount() != null
                    && subtotal.compareTo(promotion.getMinOrderAmount()) < 0) {
                return BigDecimal.ZERO;
            }

            BigDecimal discount;
            if ("PERCENTAGE".equalsIgnoreCase(promotion.getType())) {
                discount = subtotal.multiply(promotion.getValue().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                        .setScale(2, RoundingMode.HALF_UP);
                if (promotion.getMaxDiscount() != null && discount.compareTo(promotion.getMaxDiscount()) > 0) {
                    discount = promotion.getMaxDiscount();
                }
            } else if ("FIXED_AMOUNT".equalsIgnoreCase(promotion.getType()) || "FIXED".equalsIgnoreCase(promotion.getType())) {
                discount = promotion.getValue();
            } else {
                discount = BigDecimal.ZERO;
            }

            if (discount.compareTo(BigDecimal.ZERO) < 0) {
                return BigDecimal.ZERO;
            }
            if (discount.compareTo(subtotal) > 0) {
                return subtotal;
            }
            return discount;
        }

        return BigDecimal.ZERO;
    }
}
