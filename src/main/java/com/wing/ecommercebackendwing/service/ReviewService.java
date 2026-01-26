package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.request.review.CreateReviewRequest;
import com.wing.ecommercebackendwing.dto.response.review.ReviewResponse;
import com.wing.ecommercebackendwing.repository.ProductRepository;
import com.wing.ecommercebackendwing.repository.ReviewRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse createReview(UUID userId, CreateReviewRequest request) {
        // TODO: Create product review
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<ReviewResponse> getProductReviews(UUID productId) {
        // TODO: Get reviews for product
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
