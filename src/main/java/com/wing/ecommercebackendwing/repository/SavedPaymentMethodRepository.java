package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.SavedPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedPaymentMethodRepository extends JpaRepository<SavedPaymentMethod, UUID> {
    List<SavedPaymentMethod> findByUserId(UUID userId);
    Optional<SavedPaymentMethod> findByUserIdAndIsDefaultTrue(UUID userId);
}
