package com.wing.ecommercebackendwing.security;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

/**
 * Custom UserDetails implementation that includes user ID
 */
public interface CustomUserDetails extends UserDetails {
    UUID getUserId();
}
