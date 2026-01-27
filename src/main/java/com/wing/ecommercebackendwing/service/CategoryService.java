package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.CategoryMapper;
import com.wing.ecommercebackendwing.dto.response.product.CategoryResponse;
import com.wing.ecommercebackendwing.model.entity.Category;
import com.wing.ecommercebackendwing.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Category not found with slug: " + slug));
        return CategoryMapper.toResponse(category);
    }
}
