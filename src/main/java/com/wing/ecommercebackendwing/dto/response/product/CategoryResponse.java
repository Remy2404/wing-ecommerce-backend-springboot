package com.wing.ecommercebackendwing.dto.response.product;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CategoryResponse {
    private UUID id;
    private String name;
    private String slug;
    private String icon;
    private String image;
}
