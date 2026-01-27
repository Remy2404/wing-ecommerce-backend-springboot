package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.ProductMapper;
import com.wing.ecommercebackendwing.dto.request.product.CreateProductRequest;
import com.wing.ecommercebackendwing.dto.request.product.ProductFilterRequest;
import com.wing.ecommercebackendwing.dto.request.product.UpdateProductRequest;
import com.wing.ecommercebackendwing.dto.response.product.ProductResponse;
import com.wing.ecommercebackendwing.model.entity.Category;
import com.wing.ecommercebackendwing.model.entity.Product;
import com.wing.ecommercebackendwing.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepository;

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
            throw new IllegalArgumentException("Slug cannot be empty");
        }
        
        Product product = productRepository.findBySlug(slug.trim())
                .orElseThrow(() -> new RuntimeException("Product not found with slug: " + slug));
        return ProductMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product product = Product.builder()
                .name(request.getName())
                .slug(generateSlug(request.getName()))
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .stockQuantity(request.getStock())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build(); 

        Product savedProduct = productRepository.save(product);
        log.info("Created product: {} with slug: {}", savedProduct.getName(), savedProduct.getSlug());
        return ProductMapper.toResponse(savedProduct);
    }

    @Transactional
    public void updateStock(UUID productId, int quantityChange) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        int newStock = product.getStockQuantity() + quantityChange;
        if (newStock < 0) {
            throw new RuntimeException("Insufficient stock for product: " + product.getName());
        }
        
        product.setStockQuantity(newStock);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
        
        log.info("Updated stock for product {}: {} -> {}", productId, product.getStockQuantity() - quantityChange, newStock);
    }

    @Transactional
    public ProductResponse updateProduct(String slug, UpdateProductRequest request) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Product not found with slug: " + slug));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStock());
        product.setUpdatedAt(Instant.now());

        Product savedProduct = productRepository.save(product);
        return ProductMapper.toResponse(savedProduct);
    }

    private String generateSlug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-") + "-" + UUID.randomUUID().toString().substring(0, 5);
    }
}
