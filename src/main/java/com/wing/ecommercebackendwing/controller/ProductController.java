package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.product.CreateProductRequest;
import com.wing.ecommercebackendwing.dto.request.product.ProductFilterRequest;
import com.wing.ecommercebackendwing.dto.request.product.UpdateProductRequest;
import com.wing.ecommercebackendwing.dto.response.product.ProductResponse;
import com.wing.ecommercebackendwing.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get products with filtering and pagination")
    public ResponseEntity<Page<ProductResponse>> getProducts(@ModelAttribute ProductFilterRequest filter) {
        Page<ProductResponse> response = productService.getProducts(filter);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get product by slug")
    public ResponseEntity<ProductResponse> getProductBySlug(@PathVariable String slug) {
        ProductResponse response = productService.getProductBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create new product (Admin only)")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update product (Admin only)")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String slug,
            @Valid @RequestBody UpdateProductRequest updateRequest) {
        ProductResponse response = productService.updateProduct(slug, updateRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete product (Admin only)")
    public ResponseEntity<Void> deleteProduct(@PathVariable String slug) {
        productService.deleteProduct(slug);
        return ResponseEntity.ok().build();
    }
}
