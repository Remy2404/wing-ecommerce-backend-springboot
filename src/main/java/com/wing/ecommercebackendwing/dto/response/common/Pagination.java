package com.wing.ecommercebackendwing.dto.response.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Pagination {
    private Integer page;
    private Integer limit;
    private Long total;
}
