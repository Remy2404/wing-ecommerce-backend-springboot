package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.AddressMapper;
import com.wing.ecommercebackendwing.dto.request.address.CreateAddressRequest;
import com.wing.ecommercebackendwing.dto.request.address.UpdateAddressRequest;
import com.wing.ecommercebackendwing.dto.response.order.AddressResponse;
import com.wing.ecommercebackendwing.exception.custom.BadRequestException;
import com.wing.ecommercebackendwing.model.entity.Address;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.AddressRepository;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {
    private static final Set<String> HIDDEN_SETTINGS_LABELS = Set.of(
            "ORDER DELIVERY",
            "ARCHIVED_ORDER_DELIVERY"
    );

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PhoneNumberService phoneNumberService;

    public List<AddressResponse> getUserAddresses(UUID userId) {
        Set<UUID> inUseAddressIds = new HashSet<>(orderRepository.findDeliveryAddressIdsByUserId(userId));
        return addressRepository.findByUserId(userId).stream()
                .filter(address -> !isHiddenFromSettings(address.getLabel()))
                .map(address -> AddressMapper.toResponse(address, inUseAddressIds.contains(address.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse createAddress(UUID userId, CreateAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Address address = new Address();
        address.setUser(user);
        address.setLabel(request.getLabel());
        address.setFullName(request.getFullName());
        address.setPhone(phoneNumberService.normalizeToE164(request.getPhone(), request.getCountry()));
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setProvince(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        address.setCreatedAt(Instant.now());
        address.setUpdatedAt(Instant.now());

        Address saved = addressRepository.save(address);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            setDefaultAddress(userId, saved.getId());
        }

        return AddressMapper.toResponse(saved);
    }

    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, UpdateAddressRequest request) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Address does not belong to user");
        }

        if (request.getLabel() != null) address.setLabel(requireNonBlank(request.getLabel(), "label"));
        if (request.getFullName() != null) address.setFullName(requireNonBlank(request.getFullName(), "fullName"));
        if (request.getPhone() != null) {
            String phoneInput = requireNonBlank(request.getPhone(), "phone");
            String countryContext = request.getCountry() != null
                    ? requireNonBlank(request.getCountry(), "country")
                    : address.getCountry();
            address.setPhone(phoneNumberService.normalizeToE164(phoneInput, countryContext));
        }
        if (request.getStreet() != null) address.setStreet(requireNonBlank(request.getStreet(), "street"));
        if (request.getCity() != null) address.setCity(requireNonBlank(request.getCity(), "city"));
        if (request.getState() != null) address.setProvince(requireNonBlank(request.getState(), "state"));
        if (request.getPostalCode() != null) address.setPostalCode(requireNonBlank(request.getPostalCode(), "postalCode"));
        if (request.getCountry() != null) address.setCountry(requireNonBlank(request.getCountry(), "country"));
        if (request.getIsDefault() != null) address.setIsDefault(request.getIsDefault());

        address.setUpdatedAt(Instant.now());
        Address saved = addressRepository.save(address);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            setDefaultAddress(userId, saved.getId());
        }

        return AddressMapper.toResponse(saved);
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Address does not belong to user");
        }

        if (orderRepository.existsByDeliveryAddressId(addressId)) {
            throw new BadRequestException("Cannot delete an address that is used by an existing order.");
        }

        addressRepository.delete(address);
    }

    @Transactional
    public AddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Address does not belong to user");
        }

        List<Address> addresses = addressRepository.findByUserId(userId);
        for (Address addr : addresses) {
            addr.setIsDefault(addr.getId().equals(addressId));
            addr.setUpdatedAt(Instant.now());
        }
        addressRepository.saveAll(addresses);

        return AddressMapper.toResponse(address);
    }

    private String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException("Invalid value for " + field);
        }
        return value.trim();
    }

    private boolean isHiddenFromSettings(String label) {
        if (label == null) {
            return false;
        }
        return HIDDEN_SETTINGS_LABELS.contains(label.trim().toUpperCase());
    }
}
