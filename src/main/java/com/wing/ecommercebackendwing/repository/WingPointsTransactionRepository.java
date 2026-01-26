package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.WingPointsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WingPointsTransactionRepository extends JpaRepository<WingPointsTransaction, UUID> {
    List<WingPointsTransaction> findByUserId(UUID userId);
}
