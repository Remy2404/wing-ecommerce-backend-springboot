package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.ReviewMapper;
import com.wing.ecommercebackendwing.dto.request.review.CreateReviewRequest;
import com.wing.ecommercebackendwing.dto.response.review.ReviewResponse;
import com.wing.ecommercebackendwing.model.entity.Product;
import com.wing.ecommercebackendwing.model.entity.Review;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.ProductRepository;
import com.wing.ecommercebackendwing.repository.ReviewRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse createReview(UUID userId, CreateReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            review.setImages(String.join(",", request.getImages()));
        }

        review.setCreatedAt(Instant.now());
        review.setUpdatedAt(Instant.now());

        Review savedReview = reviewRepository.save(review);
        return ReviewMapper.toResponse(savedReview);
    }

    public List<ReviewResponse> getProductReviews(UUID productId) {
        return reviewRepository.findByProductId(productId).stream()
                .map(ReviewMapper::toResponse)
                .collect(Collectors.toList());
    }
}
