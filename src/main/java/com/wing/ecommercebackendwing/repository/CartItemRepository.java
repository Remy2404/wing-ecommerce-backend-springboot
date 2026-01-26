package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByCartId(UUID cartId);
    void deleteByCartIdAndProductId(UUID cartId, UUID productId);
}
