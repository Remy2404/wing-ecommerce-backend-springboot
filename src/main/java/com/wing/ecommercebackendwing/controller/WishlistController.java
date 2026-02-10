package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.order.WishlistRequest;
import com.wing.ecommercebackendwing.dto.response.product.ProductResponse;
import com.wing.ecommercebackendwing.security.CustomUserDetails;
import com.wing.ecommercebackendwing.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<List<ProductResponse>> getWishlist(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ProductResponse> response = wishlistService.getWishlist(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<ProductResponse> addToWishlist(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                @Valid @RequestBody WishlistRequest addRequest) {
        ProductResponse response = wishlistService.addToWishlist(userDetails.getUserId(), addRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/{productId}")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<Void> removeFromWishlist(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  @PathVariable(name = "productId") UUID productId) {
        wishlistService.removeFromWishlist(userDetails.getUserId(), productId);
        return ResponseEntity.ok().build();
    }
}
