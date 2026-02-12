package com.wing.ecommercebackendwing.dto.mapper;

import com.wing.ecommercebackendwing.dto.response.cart.CartItemResponse;
import com.wing.ecommercebackendwing.dto.response.cart.CartResponse;
import com.wing.ecommercebackendwing.model.entity.Cart;
import com.wing.ecommercebackendwing.model.entity.CartItem;
import com.wing.ecommercebackendwing.model.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class CartMapper {

    public static CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(CartMapper::toCartItemResponse)
                .collect(Collectors.toList());

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemCount = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .items(itemResponses)
                .itemCount(itemCount)
                .subtotal(subtotal)
                .total(subtotal)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private static CartItemResponse toCartItemResponse(CartItem cartItem) {
        BigDecimal subtotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        ProductVariant variant = cartItem.getVariant();
        Integer availableStock = variant != null
                ? variant.getStock()
                : cartItem.getProduct().getStockQuantity();

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProduct().getId())
                .merchantId(
                        cartItem.getProduct().getMerchant() != null
                                ? cartItem.getProduct().getMerchant().getId()
                                : null
                )
                .productName(cartItem.getProduct().getName())
                .variantId(variant != null ? variant.getId() : null)
                .variantName(variant != null ? variant.getName() : null)
                .quantity(cartItem.getQuantity())
                .availableStock(availableStock)
                .price(cartItem.getPrice())
                .subtotal(subtotal)
                .productSlug(cartItem.getProduct().getSlug())
                .productImage(extractFirstImage(cartItem.getProduct().getImages()))
                .build();
    }

    private static String extractFirstImage(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) {
            return null;
        }
        try {
            String clean = imagesJson.trim();
            if (clean.startsWith("[\"") && clean.endsWith("\"]")) {
                int endIndex = clean.indexOf("\",");
                if (endIndex == -1) {
                    endIndex = clean.lastIndexOf("\"]");
                }
                return clean.substring(2, endIndex);
            } else if (clean.startsWith("[") && clean.endsWith("]")) {
                 return null;
            }
            return clean; 
        } catch (Exception e) {
            return null;
        }
    }
}
