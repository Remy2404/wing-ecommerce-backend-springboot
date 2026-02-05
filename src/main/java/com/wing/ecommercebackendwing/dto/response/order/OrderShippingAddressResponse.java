package com.wing.ecommercebackendwing.dto.response.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderShippingAddressResponse {
    private String fullName;
    private String phone;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
