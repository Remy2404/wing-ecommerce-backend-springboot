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

    @Transactional
    public PromotionResponse createPromotion(com.wing.ecommercebackendwing.dto.request.promotion.PromotionCreateRequest request) {
        if (promotionRepository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Promotion code already exists");
        }
        Promotion promotion = PromotionMapper.toEntity(request);
        promotion.setUsedCount(0);
        Promotion savedPromotion = promotionRepository.save(promotion);
        return PromotionMapper.toResponse(savedPromotion);
    }

    public java.util.List<PromotionResponse> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(PromotionMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    public PromotionResponse getPromotionById(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
        return PromotionMapper.toResponse(promotion);
    }

    @Transactional
    public PromotionResponse updatePromotion(UUID id, com.wing.ecommercebackendwing.dto.request.promotion.PromotionUpdateRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));

        if (request.getName() != null) promotion.setName(request.getName());
        if (request.getDescription() != null) promotion.setDescription(request.getDescription());
        if (request.getType() != null) promotion.setType(request.getType());
        if (request.getValue() != null) promotion.setValue(request.getValue());
        if (request.getMinOrderAmount() != null) promotion.setMinOrderAmount(request.getMinOrderAmount());
        if (request.getMaxDiscount() != null) promotion.setMaxDiscount(request.getMaxDiscount());
        if (request.getUsageLimit() != null) promotion.setUsageLimit(request.getUsageLimit());
        if (request.getPerUserLimit() != null) promotion.setPerUserLimit(request.getPerUserLimit());
        if (request.getStartDate() != null) promotion.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) promotion.setEndDate(request.getEndDate());
        if (request.getIsActive() != null) promotion.setIsActive(request.getIsActive());
        if (request.getApplicableCategories() != null) promotion.setApplicableCategories(request.getApplicableCategories());
        if (request.getApplicableMerchants() != null) promotion.setApplicableMerchants(request.getApplicableMerchants());

        Promotion savedPromotion = promotionRepository.save(promotion);
        return PromotionMapper.toResponse(savedPromotion);
    }

    @Transactional
    public void deletePromotion(UUID id) {
        if (!promotionRepository.existsById(id)) {
            throw new RuntimeException("Promotion not found");
        }
        promotionRepository.deleteById(id);
    }

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

        // Calculate discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if ("PERCENTAGE".equalsIgnoreCase(promotion.getType())) {
            discountAmount = order.getTotalAmount().multiply(promotion.getValue().divide(new BigDecimal(100)));
            if (promotion.getMaxDiscount() != null && discountAmount.compareTo(promotion.getMaxDiscount()) > 0) {
                discountAmount = promotion.getMaxDiscount();
            }
        } else if ("FIXED_AMOUNT".equalsIgnoreCase(promotion.getType()) || "FIXED".equalsIgnoreCase(promotion.getType())) {
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
