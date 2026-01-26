package com.wing.ecommercebackendwing.dto.response.review;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID productId;
    private UUID userId;
    private Integer rating;
    private String comment;
    private String images;
    private Instant createdAt;
}
