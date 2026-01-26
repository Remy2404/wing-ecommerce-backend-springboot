package com.wing.ecommercebackendwing.dto.response.order;

import com.wing.ecommercebackendwing.dto.response.common.Pagination;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderListResponse {
    private List<OrderResponse> orders;
    private Pagination pagination;
}
