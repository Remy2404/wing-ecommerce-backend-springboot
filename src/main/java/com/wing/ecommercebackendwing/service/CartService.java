package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.CartMapper;
import com.wing.ecommercebackendwing.dto.request.cart.AddToCartRequest;
import com.wing.ecommercebackendwing.dto.request.cart.UpdateCartItemRequest;
import com.wing.ecommercebackendwing.dto.response.cart.CartResponse;
import com.wing.ecommercebackendwing.model.entity.Cart;
import com.wing.ecommercebackendwing.model.entity.CartItem;
import com.wing.ecommercebackendwing.model.entity.Product;
import com.wing.ecommercebackendwing.model.entity.ProductVariant;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.CartRepository;
import com.wing.ecommercebackendwing.repository.ProductRepository;
import com.wing.ecommercebackendwing.repository.ProductVariantRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartResponse getCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        refreshCartPricing(cart);
        return CartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(UUID userId, AddToCartRequest request) {
        int quantityToAdd = request.getQuantity() != null ? request.getQuantity() : 0;
        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        ProductVariant variant = resolveVariant(product, request.getVariantId());

        int availableStock = resolveAvailableStock(product, variant);
        BigDecimal currentPrice = resolveCurrentPrice(product, variant);

        // Check if item already exists in cart
        CartItem existingItem = findMatchingCartItem(
                cart,
                product.getId(),
                variant != null ? variant.getId() : null
        ).orElse(null);

        if (existingItem != null) {
            // Update quantity
            int newQuantity = existingItem.getQuantity() + quantityToAdd;
            if (availableStock < newQuantity) {
                throw new RuntimeException("Insufficient stock available");
            }
            existingItem.setQuantity(newQuantity);
            existingItem.setVariant(variant);
            existingItem.setPrice(currentPrice);
            existingItem.setUpdatedAt(Instant.now());
        } else {
            // Create new cart item
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setVariant(variant);
            cartItem.setQuantity(quantityToAdd);
            cartItem.setPrice(currentPrice);
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
        int requestedQuantity = request.getQuantity() != null ? request.getQuantity() : 0;
        if (requestedQuantity <= 0) {
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
        ProductVariant variant = cartItem.getVariant();
        int availableStock = resolveAvailableStock(product, variant);
        if (availableStock < requestedQuantity) {
            throw new RuntimeException("Insufficient stock available");
        }

        cartItem.setQuantity(requestedQuantity);
        cartItem.setPrice(resolveCurrentPrice(product, variant));
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

    private ProductVariant resolveVariant(Product product, UUID variantId) {
        if (variantId == null) {
            return null;
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));
        if (!variant.getProduct().getId().equals(product.getId())) {
            throw new RuntimeException("Variant does not belong to the specified product");
        }
        return variant;
    }

    private Optional<CartItem> findMatchingCartItem(Cart cart, UUID productId, UUID variantId) {
        return cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .filter(item -> Objects.equals(
                        item.getVariant() != null ? item.getVariant().getId() : null,
                        variantId
                ))
                .findFirst();
    }

    private int resolveAvailableStock(Product product, ProductVariant variant) {
        if (variant != null) {
            return variant.getStock() != null ? variant.getStock() : 0;
        }
        return product.getStockQuantity() != null ? product.getStockQuantity() : 0;
    }

    private BigDecimal resolveCurrentPrice(Product product, ProductVariant variant) {
        if (variant != null && variant.getPrice() != null) {
            return variant.getPrice();
        }
        return product.getPrice();
    }

    private void refreshCartPricing(Cart cart) {
        boolean changed = false;
        Instant now = Instant.now();

        for (CartItem item : cart.getItems()) {
            BigDecimal currentPrice = resolveCurrentPrice(item.getProduct(), item.getVariant());
            if (item.getPrice() == null || item.getPrice().compareTo(currentPrice) != 0) {
                item.setPrice(currentPrice);
                item.setUpdatedAt(now);
                changed = true;
            }
        }

        if (changed) {
            cart.setUpdatedAt(now);
            cartRepository.save(cart);
        }
    }
}
