package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.WishlistMapper;
import com.wing.ecommercebackendwing.dto.request.order.WishlistRequest;
import com.wing.ecommercebackendwing.dto.response.product.ProductResponse;
import com.wing.ecommercebackendwing.model.entity.Product;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.model.entity.Wishlist;
import com.wing.ecommercebackendwing.repository.ProductRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import com.wing.ecommercebackendwing.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProductResponse addToWishlist(UUID userId, WishlistRequest request) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new RuntimeException("Product already in wishlist");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);
        wishlist.setCreatedAt(Instant.now());

        Wishlist saved = wishlistRepository.save(wishlist);
        return WishlistMapper.toProductResponse(saved);
    }

    @Transactional
    public void removeFromWishlist(UUID userId, UUID productId) {
        List<Wishlist> wishlistItems = wishlistRepository.findByUserId(userId);
        wishlistItems.stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(wishlistRepository::delete);
    }

    public List<ProductResponse> getWishlist(UUID userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(WishlistMapper::toProductResponse)
                .collect(Collectors.toList());
    }
}
