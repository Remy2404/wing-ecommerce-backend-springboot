package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant,Integer> {
    Optional<Merchant> findByUserId(UUID userId);
}
