package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.response.product.CategoryResponse;
import com.wing.ecommercebackendwing.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getCategories() {
        // TODO: Get all categories
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public CategoryResponse getCategoryBySlug(String slug) {
        // TODO: Find category by slug
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
