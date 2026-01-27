package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.order.WishlistRequest;
import com.wing.ecommercebackendwing.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Wishlist management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @Operation(summary = "Get user's wishlist")
    public ResponseEntity<Object> getWishlist(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Object response = wishlistService.getWishlist(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<Object> addToWishlist(Authentication authentication,
                                                @RequestBody WishlistRequest addRequest) {
        UUID userId = UUID.fromString(authentication.getName());
        Object response = wishlistService.addToWishlist(userId, addRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/{productId}")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<Void> removeFromWishlist(Authentication authentication,
                                                   @PathVariable UUID productId) {
        UUID userId = UUID.fromString(authentication.getName());
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok().build();
    }
}
