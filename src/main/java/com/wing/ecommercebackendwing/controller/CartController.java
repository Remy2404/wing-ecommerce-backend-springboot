package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.cart.AddToCartRequest;
import com.wing.ecommercebackendwing.dto.request.cart.UpdateCartItemRequest;
import com.wing.ecommercebackendwing.dto.response.cart.CartResponse;
import com.wing.ecommercebackendwing.dto.response.common.ValidationErrorResponse;
import com.wing.ecommercebackendwing.security.CustomUserDetails;
import com.wing.ecommercebackendwing.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        CartResponse response = cartService.getCart(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    @Operation(summary = "Add item to cart")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item added to cart successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CartResponse> addToCart(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  @Valid @RequestBody AddToCartRequest request) {
        CartResponse response = cartService.addToCart(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items")
    @Operation(summary = "Update cart item quantity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cart item updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CartResponse> updateQuantity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse response = cartService.updateQuantity(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/{itemId}")
    @Operation(summary = "Remove item from cart")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item removed from cart successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Cart item ID to remove", required = true)
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
