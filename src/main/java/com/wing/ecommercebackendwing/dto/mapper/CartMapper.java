package com.wing.ecommercebackendwing.dto.mapper;

import com.wing.ecommercebackendwing.dto.response.cart.CartItemResponse;
import com.wing.ecommercebackendwing.dto.response.cart.CartResponse;
import com.wing.ecommercebackendwing.model.entity.Cart;
import com.wing.ecommercebackendwing.model.entity.CartItem;

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
                .items(itemResponses)
                .subtotal(subtotal)
                .itemCount(itemCount)
                .build();
    }

    private static CartItemResponse toCartItemResponse(CartItem cartItem) {
        BigDecimal subtotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProduct().getId())
                .productName(cartItem.getProduct().getName())
                .variantId(cartItem.getVariant() != null ? cartItem.getVariant().getId() : null)
                .variantName(cartItem.getVariant() != null ? cartItem.getVariant().getName() : null)
                .quantity(cartItem.getQuantity())
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
