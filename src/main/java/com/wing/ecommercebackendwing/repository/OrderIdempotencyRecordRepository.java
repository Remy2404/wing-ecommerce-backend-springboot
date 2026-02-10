package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.OrderIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderIdempotencyRecordRepository extends JpaRepository<OrderIdempotencyRecord, UUID> {
    Optional<OrderIdempotencyRecord> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
