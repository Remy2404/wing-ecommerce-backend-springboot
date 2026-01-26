package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.ProductMapper;
import com.wing.ecommercebackendwing.dto.request.product.CreateProductRequest;
import com.wing.ecommercebackendwing.dto.request.product.ProductFilterRequest;
import com.wing.ecommercebackendwing.dto.response.product.ProductResponse;
import com.wing.ecommercebackendwing.model.entity.Product;
import com.wing.ecommercebackendwing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public Page<ProductResponse> getProducts(ProductFilterRequest filter) {
        Pageable pageable = PageRequest.of(
                filter.getPage(),
                Math.min(filter.getSize(), 100) // Max 100 items per page
        );

        // Apply filters (simplified - should use Specification for complex filtering)
        Page<Product> products;
        if (filter.getCategoryId() != null) {
            products = productRepository.findByCategoryId(filter.getCategoryId(), pageable);
        } else if (filter.getSearchQuery() != null && !filter.getSearchQuery().isBlank()) {
            products = productRepository.searchByNameOrDescription(filter.getSearchQuery().trim(), pageable);
        } else {
            products = productRepository.findAll(pageable);
        }

        return products.map(ProductMapper::toResponse);
    }

    public ProductResponse getProductBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Product slug cannot be empty");
        }

        Product product = productRepository.findBySlug(slug.trim())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        return ProductMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        // Check if slug already exists
        if (productRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException("Product with this slug already exists");
        }

        Product product = ProductMapper.toEntity(request);
        product.setCreatedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        
        Product savedProduct = productRepository.save(product);
        log.info("Created new product: {}", savedProduct.getId());
        
        return ProductMapper.toResponse(savedProduct);
    }

    @Transactional
    public void updateStock(UUID productId, int quantityChange) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        int newStock = product.getStockQuantity() + quantityChange;
        if (newStock < 0) {
            throw new RuntimeException("Insufficient stock");
        }

        product.setStockQuantity(newStock);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
        
        log.info("Updated stock for product {}: {} -> {}", productId, product.getStockQuantity() - quantityChange, newStock);
    }
}
