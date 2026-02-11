package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.WingPointsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WingPointsTransactionRepository extends JpaRepository<WingPointsTransaction, UUID> {
    List<WingPointsTransaction> findByUserId(UUID userId);
    boolean existsByUserIdAndOrderIdAndType(UUID userId, UUID orderId, String type);
    Optional<WingPointsTransaction> findFirstByUserIdAndOrderIdAndType(UUID userId, UUID orderId, String type);
}
