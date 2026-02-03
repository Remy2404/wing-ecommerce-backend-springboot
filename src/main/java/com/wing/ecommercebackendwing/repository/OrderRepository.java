package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserId(UUID userId);
    Page<Order> findByUserId(UUID userId, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
    long countByUserId(UUID userId);
    Optional<Order> findFirstByOrderNumberStartingWithOrderByOrderNumberDesc(String prefix);
    Page<Order> findByMerchantId(UUID merchantId, Pageable pageable);


    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    BigDecimal sumTotalAmount();
}
