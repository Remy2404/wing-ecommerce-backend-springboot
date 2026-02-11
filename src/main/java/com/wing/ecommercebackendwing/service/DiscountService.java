package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.model.entity.Promotion;
import com.wing.ecommercebackendwing.model.entity.PromotionUsage;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.model.entity.Order;
import com.wing.ecommercebackendwing.repository.PromotionUsageRepository;
import com.wing.ecommercebackendwing.repository.PromotionRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final UserRepository userRepository;

    public BigDecimal calculateDiscount(BigDecimal subtotal, String couponCode) {
        return resolveDiscountInternal(subtotal, couponCode).amount();
    }

    @Transactional
    public AppliedDiscount resolveDiscountForCheckout(BigDecimal subtotal, String couponCode, UUID userId) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0 || couponCode == null || userId == null) {
            return AppliedDiscount.none();
        }
        String code = couponCode.toUpperCase().trim();
        if (code.isEmpty()) {
            return AppliedDiscount.none();
        }

        Promotion promotion = promotionRepository
                .findByCodeIgnoreCaseForUpdate(code)
                .orElse(null);
        if (promotion == null) {
            return AppliedDiscount.none();
        }

        if (!isPromotionEligible(promotion, subtotal)) {
            return AppliedDiscount.none();
        }

        if (promotion.getUsageLimit() != null) {
            int usedCount = promotion.getUsedCount() != null ? promotion.getUsedCount() : 0;
            if (usedCount >= promotion.getUsageLimit()) {
                return AppliedDiscount.none();
            }
        }

        if (promotion.getPerUserLimit() != null && promotion.getPerUserLimit() > 0) {
            int userUsage = promotionUsageRepository.countByPromotionIdAndUserId(promotion.getId(), userId);
            if (userUsage >= promotion.getPerUserLimit()) {
                return AppliedDiscount.none();
            }
        }

        BigDecimal discount = calculateDiscountAmount(promotion, subtotal);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return AppliedDiscount.none();
        }
        return new AppliedDiscount(promotion, discount, code);
    }

    @Transactional
    public void recordPromotionUsage(AppliedDiscount appliedDiscount, UUID userId, Order order) {
        if (appliedDiscount == null || !appliedDiscount.hasPromotion() || userId == null || order == null) {
            return;
        }

        Promotion promotion = appliedDiscount.promotion();
        if (promotionUsageRepository.existsByPromotionIdAndOrderId(promotion.getId(), order.getId())) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        PromotionUsage usage = new PromotionUsage();
        usage.setPromotion(promotion);
        usage.setUser(user);
        usage.setOrder(order);
        usage.setDiscountAmount(appliedDiscount.amount());
        usage.setCreatedAt(Instant.now());
        promotionUsageRepository.save(usage);

        int currentUsed = promotion.getUsedCount() != null ? promotion.getUsedCount() : 0;
        promotion.setUsedCount(currentUsed + 1);
        promotionRepository.save(promotion);
    }

    private AppliedDiscount resolveDiscountInternal(BigDecimal subtotal, String couponCode) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0 || couponCode == null) {
            return AppliedDiscount.none();
        }

        String code = couponCode.toUpperCase().trim();
        if (code.isEmpty()) {
            return AppliedDiscount.none();
        }

        Promotion promotion = promotionRepository.findByCodeIgnoreCase(code).orElse(null);
        if (promotion == null) {
            return AppliedDiscount.none();
        }

        if (!isPromotionEligible(promotion, subtotal)) {
            return AppliedDiscount.none();
        }

        BigDecimal discount = calculateDiscountAmount(promotion, subtotal);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return AppliedDiscount.none();
        }

        return new AppliedDiscount(promotion, discount, code);
    }

    private boolean isPromotionEligible(Promotion promotion, BigDecimal subtotal) {
        Instant now = Instant.now();
        if (!Boolean.TRUE.equals(promotion.getIsActive())) {
            return false;
        }
        if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) {
            return false;
        }
        if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
            return false;
        }
        return promotion.getMinOrderAmount() == null || subtotal.compareTo(promotion.getMinOrderAmount()) >= 0;
    }

    private BigDecimal calculateDiscountAmount(Promotion promotion, BigDecimal subtotal) {
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

    public record AppliedDiscount(Promotion promotion, BigDecimal amount, String couponCode) {
        public static AppliedDiscount none() {
            return new AppliedDiscount(null, BigDecimal.ZERO, null);
        }

        public boolean hasPromotion() {
            return promotion != null && amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
        }
    }
}
