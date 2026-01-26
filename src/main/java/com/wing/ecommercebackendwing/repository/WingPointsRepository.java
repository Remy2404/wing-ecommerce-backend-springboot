package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.WingPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WingPointsRepository extends JpaRepository<WingPoints, UUID> {
    Optional<WingPoints> findByUserId(UUID userId);
}
