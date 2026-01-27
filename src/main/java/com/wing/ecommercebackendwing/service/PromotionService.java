package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.PromotionMapper;
import com.wing.ecommercebackendwing.dto.request.promotion.PromotionApplyRequest;
import com.wing.ecommercebackendwing.dto.response.promotion.PromotionResponse;
import com.wing.ecommercebackendwing.model.entity.Order;
import com.wing.ecommercebackendwing.model.entity.Promotion;
import com.wing.ecommercebackendwing.model.entity.PromotionUsage;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.PromotionRepository;
import com.wing.ecommercebackendwing.repository.PromotionUsageRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public PromotionResponse validatePromotion(String code, UUID userId) {
        Promotion promotion = promotionRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid promotion code"));

        if (!promotion.getIsActive()) {
            throw new RuntimeException("Promotion is not active");
        }

        Instant now = Instant.now();
        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new RuntimeException("Promotion has expired or not yet started");
        }

        if (promotion.getUsageLimit() != null && promotion.getUsedCount() >= promotion.getUsageLimit()) {
            throw new RuntimeException("Promotion usage limit reached");
        }

        if (promotion.getPerUserLimit() != null) {
            int userUsage = promotionUsageRepository.countByPromotionIdAndUserId(promotion.getId(), userId);
            if (userUsage >= promotion.getPerUserLimit()) {
                throw new RuntimeException("You have reached the usage limit for this promotion");
            }
        }

        return PromotionMapper.toResponse(promotion);
    }

    @Transactional
    public PromotionResponse applyDiscount(UUID userId, PromotionApplyRequest request) {
        PromotionResponse promotionResponse = validatePromotion(request.getCode(), userId);
        Promotion promotion = promotionRepository.findById(promotionResponse.getId()).get();
        
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        User user = userRepository.findById(userId).get();

        // Calculate discount (simplified for now, assuming percentage type for fixed type logic)
        BigDecimal discountAmount = BigDecimal.ZERO;
        if ("PERCENTAGE".equalsIgnoreCase(promotion.getType())) {
            discountAmount = order.getTotalAmount().multiply(promotion.getValue().divide(new BigDecimal(100)));
            if (promotion.getMaxDiscount() != null && discountAmount.compareTo(promotion.getMaxDiscount()) > 0) {
                discountAmount = promotion.getMaxDiscount();
            }
        } else if ("FIXED".equalsIgnoreCase(promotion.getType())) {
            discountAmount = promotion.getValue();
        }

        PromotionUsage usage = new PromotionUsage();
        usage.setPromotion(promotion);
        usage.setUser(user);
        usage.setOrder(order);
        usage.setDiscountAmount(discountAmount);
        usage.setCreatedAt(Instant.now());

        promotionUsageRepository.save(usage);
        
        promotion.setUsedCount(promotion.getUsedCount() + 1);
        promotionRepository.save(promotion);

        // Update order total amount (simplified)
        order.setTotalAmount(order.getTotalAmount().subtract(discountAmount));
        orderRepository.save(order);

        return promotionResponse;
    }
}
