package com.wing.ecommercebackendwing.dto.mapper;

import com.wing.ecommercebackendwing.dto.response.product.ProductResponse;
import com.wing.ecommercebackendwing.model.entity.Wishlist;

public class WishlistMapper {
    public static ProductResponse toProductResponse(Wishlist wishlist) {
        return ProductMapper.toResponse(wishlist.getProduct());
    }
}
