package com.wing.ecommercebackendwing.controller;

import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerValidationAnnotationTest {

    @Test
    void updateProfile_shouldRequireValidRequestBody() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("updateProfile",
                com.wing.ecommercebackendwing.security.CustomUserDetails.class,
                com.wing.ecommercebackendwing.dto.request.user.UpdateProfileRequest.class);
        assertTrue(hasValidAnnotation(method, 1));
    }

    @Test
    void updateAddress_shouldRequireValidRequestBody() throws NoSuchMethodException {
        Method method = AddressController.class.getMethod("updateAddress",
                com.wing.ecommercebackendwing.security.CustomUserDetails.class,
                java.util.UUID.class,
                com.wing.ecommercebackendwing.dto.request.address.UpdateAddressRequest.class);
        assertTrue(hasValidAnnotation(method, 2));
    }

    @Test
    void updatePaymentMethod_shouldRequireValidRequestBody() throws NoSuchMethodException {
        Method method = SavedPaymentMethodController.class.getMethod("updateMethod",
                com.wing.ecommercebackendwing.security.CustomUserDetails.class,
                java.util.UUID.class,
                com.wing.ecommercebackendwing.dto.request.paymentmethod.UpdateSavedPaymentMethodRequest.class);
        assertTrue(hasValidAnnotation(method, 2));
    }

    @Test
    void addToWishlist_shouldRequireValidRequestBody() throws NoSuchMethodException {
        Method method = WishlistController.class.getMethod("addToWishlist",
                com.wing.ecommercebackendwing.security.CustomUserDetails.class,
                com.wing.ecommercebackendwing.dto.request.order.WishlistRequest.class);
        assertTrue(hasValidAnnotation(method, 1));
    }

    private boolean hasValidAnnotation(Method method, int parameterIndex) {
        Parameter parameter = method.getParameters()[parameterIndex];
        return Arrays.stream(parameter.getAnnotations())
                .map(Annotation::annotationType)
                .anyMatch(type -> type.equals(Valid.class));
    }
}
