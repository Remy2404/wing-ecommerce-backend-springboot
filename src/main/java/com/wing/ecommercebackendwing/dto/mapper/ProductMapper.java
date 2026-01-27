package com.wing.ecommercebackendwing.dto.mapper;

import com.wing.ecommercebackendwing.dto.request.product.CreateProductRequest;
import com.wing.ecommercebackendwing.dto.response.common.Pagination;
import com.wing.ecommercebackendwing.dto.response.product.ProductListResponse;
import com.wing.ecommercebackendwing.dto.response.product.ProductResponse;
import com.wing.ecommercebackendwing.model.entity.Product;

import java.util.List;
import java.util.stream.Collectors;

public class ProductMapper {

    public static ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .price(product.getPrice())
                .comparePrice(product.getComparePrice())
                .stock(product.getStockQuantity())
                .images(product.getImages())
                .rating(product.getRating().doubleValue())
                .build();
    }

    public static Product toEntity(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStock());
        product.setImages(request.getImages() != null ? String.join(",", request.getImages()) : null);
        return product;
    }

    public static ProductListResponse toListResponse(List<Product> products, Pagination pagination) {
        List<ProductResponse> productResponses = products.stream()
                .map(ProductMapper::toResponse)
                .collect(Collectors.toList());
        return ProductListResponse.builder()
                .products(productResponses)
                .pagination(pagination)
                .build();
    }
}
