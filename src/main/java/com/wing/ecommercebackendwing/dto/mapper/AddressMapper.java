package com.wing.ecommercebackendwing.dto.mapper;

import com.wing.ecommercebackendwing.dto.request.order.ShippingAddressRequest;
import com.wing.ecommercebackendwing.dto.response.order.AddressResponse;
import com.wing.ecommercebackendwing.model.entity.Address;

public class AddressMapper {

    public static AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .userId(address.getUser().getId())
                .label(address.getLabel())
                .fullName(address.getFullName())
                .phone(address.getPhone())
                .street(address.getStreet())
                .city(address.getCity())
                .district(address.getDistrict())
                .state(address.getProvince())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    public static Address toEntity(ShippingAddressRequest request) {
        Address address = new Address();
        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setProvince(request.getState());
        address.setCountry(request.getCountry());
        address.setPostalCode(request.getZipCode());
        return address;
    }
}
