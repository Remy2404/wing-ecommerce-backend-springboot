package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.cart.AddToCartRequest;
import com.wing.ecommercebackendwing.dto.request.cart.UpdateCartItemRequest;
import com.wing.ecommercebackendwing.dto.response.cart.CartResponse;
import com.wing.ecommercebackendwing.security.CustomUserDetails;
import com.wing.ecommercebackendwing.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get user's cart")
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        CartResponse response = cartService.getCart(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<CartResponse> addToCart(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  @Valid @RequestBody AddToCartRequest request) {
        CartResponse response = cartService.addToCart(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<CartResponse> updateQuantity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse response = cartService.updateQuantity(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/{itemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                   @PathVariable UUID itemId) {
        CartResponse response = cartService.removeItem(userDetails.getUserId(), itemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear all items from cart")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        cartService.clearCart(userDetails.getUserId());
        return ResponseEntity.ok().build();
    }
}
