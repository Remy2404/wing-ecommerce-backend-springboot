package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.repository.ProductRepository;
import com.wing.ecommercebackendwing.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Object addToWishlist(UUID userId, Object addRequest) {
        // TODO: Add product to wishlist
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public void removeFromWishlist(UUID userId, UUID productId) {
        // TODO: Remove from wishlist
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Object getWishlist(UUID userId) {
        // TODO: Get user's wishlist
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
