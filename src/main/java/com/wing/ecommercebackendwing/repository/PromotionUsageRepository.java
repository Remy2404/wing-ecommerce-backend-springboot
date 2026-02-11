package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, UUID> {
    int countByPromotionIdAndUserId(UUID promotionId, UUID userId);
    boolean existsByPromotionIdAndOrderId(UUID promotionId, UUID orderId);
}
