package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.CartMapper;
import com.wing.ecommercebackendwing.dto.request.cart.AddToCartRequest;
import com.wing.ecommercebackendwing.dto.request.cart.UpdateCartItemRequest;
import com.wing.ecommercebackendwing.dto.response.cart.CartResponse;
import com.wing.ecommercebackendwing.model.entity.Cart;
import com.wing.ecommercebackendwing.model.entity.CartItem;
import com.wing.ecommercebackendwing.model.entity.Product;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.CartItemRepository;
import com.wing.ecommercebackendwing.repository.CartRepository;
import com.wing.ecommercebackendwing.repository.ProductRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartResponse getCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        return CartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(UUID userId, AddToCartRequest request) {
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check stock availability
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock available");
        }

        // Check if item already exists in cart
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Update quantity
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newQuantity) {
                throw new RuntimeException("Insufficient stock available");
            }
            existingItem.setQuantity(newQuantity);
            existingItem.setUpdatedAt(Instant.now());
        } else {
            // Create new cart item
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(product.getPrice());
            cartItem.setCreatedAt(Instant.now());
            cartItem.setUpdatedAt(Instant.now());
            cart.getItems().add(cartItem);
        }

        cart.setUpdatedAt(Instant.now());
        Cart savedCart = cartRepository.save(cart);
        
        log.info("Added product {} to cart for user {}", product.getId(), userId);
        return CartMapper.toResponse(savedCart);
    }

    @Transactional
    public CartResponse updateQuantity(UUID userId, UpdateCartItemRequest request) {
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(request.getCartItemId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        // Verify ownership
        if (!cart.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to cart");
        }

        // Check stock availability
        Product product = cartItem.getProduct();
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock available");
        }

        cartItem.setQuantity(request.getQuantity());
        cartItem.setUpdatedAt(Instant.now());
        cart.setUpdatedAt(Instant.now());
        
        Cart savedCart = cartRepository.save(cart);
        log.info("Updated cart item {} for user {}", request.getCartItemId(), userId);
        return CartMapper.toResponse(savedCart);
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID itemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        // Verify ownership
        if (!cart.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to cart");
        }

        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new RuntimeException("Cart item not found");
        }

        cart.setUpdatedAt(Instant.now());
        Cart savedCart = cartRepository.save(cart);
        
        log.info("Removed item {} from cart for user {}", itemId, userId);
        return CartMapper.toResponse(savedCart);
    }

    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        // Verify ownership
        if (!cart.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to cart");
        }

        cart.getItems().clear();
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
        
        log.info("Cleared cart for user {}", userId);
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setCreatedAt(Instant.now());
                    newCart.setUpdatedAt(Instant.now());
                    newCart.setItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });
    }
}
