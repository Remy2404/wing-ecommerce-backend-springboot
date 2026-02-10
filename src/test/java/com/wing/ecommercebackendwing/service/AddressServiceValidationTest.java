package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.request.address.UpdateAddressRequest;
import com.wing.ecommercebackendwing.exception.custom.BadRequestException;
import com.wing.ecommercebackendwing.model.entity.Address;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.AddressRepository;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceValidationTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AddressService addressService;

    @Test
    void updateAddress_shouldReturnBadRequestForBlankPhoneWithoutSaving() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        Address address = new Address();
        address.setId(addressId);
        address.setUser(user);

        UpdateAddressRequest request = UpdateAddressRequest.builder()
                .phone("   ")
                .build();

        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        assertThrows(BadRequestException.class,
                () -> addressService.updateAddress(userId, addressId, request));

        verify(addressRepository, never()).save(any(Address.class));
    }
}
