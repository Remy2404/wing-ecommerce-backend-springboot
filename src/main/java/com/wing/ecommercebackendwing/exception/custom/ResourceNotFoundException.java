package com.wing.ecommercebackendwing.exception.custom;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String identifier) {
        super(String.format("%s not found with id: %s", resource, identifier));
    }
}
