package com.wing.ecommercebackendwing.dto.response.product;

import com.wing.ecommercebackendwing.dto.response.common.Pagination;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductListResponse {
    private List<ProductResponse> products;
    private Pagination pagination;
}
