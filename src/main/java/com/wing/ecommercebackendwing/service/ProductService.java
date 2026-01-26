package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.request.product.CreateProductRequest;
import com.wing.ecommercebackendwing.dto.request.product.ProductFilterRequest;
import com.wing.ecommercebackendwing.dto.response.product.ProductResponse;
import com.wing.ecommercebackendwing.repository.CategoryRepository;
import com.wing.ecommercebackendwing.repository.ProductRepository;
import com.wing.ecommercebackendwing.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;

    public Page<ProductResponse> getProducts(ProductFilterRequest filter) {
        // TODO: Implement product listing with filters
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public ProductResponse getProductBySlug(String slug) {
        // TODO: Find product by slug
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        // TODO: Create new product
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public void updateStock(Object updateRequest) {
        // TODO: Update product stock
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
