package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.request.cart.AddToCartRequest;
import com.wing.ecommercebackendwing.dto.response.cart.CartResponse;
import com.wing.ecommercebackendwing.repository.CartItemRepository;
import com.wing.ecommercebackendwing.repository.CartRepository;
import com.wing.ecommercebackendwing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartResponse getCart(UUID userId) {
        // TODO: Get user's cart
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public CartResponse addToCart(UUID userId, AddToCartRequest request) {
        // TODO: Add item to cart
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public CartResponse updateQuantity(UUID userId, Object updateRequest) {
        // TODO: Update cart item quantity
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID itemId) {
        // TODO: Remove item from cart
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public void clearCart(UUID userId) {
        // TODO: Clear all items from cart
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
