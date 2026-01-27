package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    List<Wishlist> findByUserId(UUID userId);
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
    long countByUserId(UUID userId);
}
