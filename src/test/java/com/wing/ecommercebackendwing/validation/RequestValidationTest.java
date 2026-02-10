package com.wing.ecommercebackendwing.validation;

import com.wing.ecommercebackendwing.dto.request.address.CreateAddressRequest;
import com.wing.ecommercebackendwing.dto.request.address.UpdateAddressRequest;
import com.wing.ecommercebackendwing.dto.request.auth.Enable2FARequest;
import com.wing.ecommercebackendwing.dto.request.auth.RegisterRequest;
import com.wing.ecommercebackendwing.dto.request.order.CreateOrderRequest;
import com.wing.ecommercebackendwing.dto.request.order.ShippingAddressRequest;
import com.wing.ecommercebackendwing.dto.request.paymentmethod.CreateSavedPaymentMethodRequest;
import com.wing.ecommercebackendwing.dto.request.paymentmethod.UpdateSavedPaymentMethodRequest;
import com.wing.ecommercebackendwing.dto.request.user.UpdateProfileRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void registerRequest_shouldAcceptLocalPhoneStartingWithZero() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("A")
                .lastName("B")
                .email("a@b.com")
                .phone("096892323")
                .password("Password1!")
                .confirmPassword("Password1!")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void enable2FARequest_shouldRejectNonDigitCode() {
        Enable2FARequest request = new Enable2FARequest();
        request.setCode("12ab56");

        Set<ConstraintViolation<Enable2FARequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void createOrderRequest_shouldAllowAddressIdWithoutShippingAddressAndItems() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddressId(UUID.randomUUID())
                .paymentMethod("KHQR")
                .build();

        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void updateProfileRequest_shouldRejectInvalidPhone() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .phoneNumber("abc###")
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void createAddressRequest_shouldRejectInvalidPhone() {
        CreateAddressRequest request = CreateAddressRequest.builder()
                .label("Home")
                .fullName("User")
                .phone("abc###")
                .street("Street")
                .city("City")
                .state("State")
                .postalCode("12345")
                .country("KH")
                .build();

        Set<ConstraintViolation<CreateAddressRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void updateAddressRequest_shouldRejectInvalidPhone() {
        UpdateAddressRequest request = UpdateAddressRequest.builder()
                .phone("abc###")
                .build();

        Set<ConstraintViolation<UpdateAddressRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shippingAddressRequest_shouldRejectInvalidPhone() {
        ShippingAddressRequest request = ShippingAddressRequest.builder()
                .fullName("User")
                .street("Street")
                .city("City")
                .state("State")
                .zipCode("12000")
                .country("KH")
                .phone("abc###")
                .build();

        Set<ConstraintViolation<ShippingAddressRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void createSavedPaymentMethodRequest_shouldAcceptLowercaseMethod() {
        CreateSavedPaymentMethodRequest request = CreateSavedPaymentMethodRequest.builder()
                .method("khqr")
                .providerToken("token")
                .build();

        Set<ConstraintViolation<CreateSavedPaymentMethodRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void updateSavedPaymentMethodRequest_shouldRejectInvalidMonth() {
        UpdateSavedPaymentMethodRequest request = UpdateSavedPaymentMethodRequest.builder()
                .expMonth(13)
                .build();

        Set<ConstraintViolation<UpdateSavedPaymentMethodRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}
