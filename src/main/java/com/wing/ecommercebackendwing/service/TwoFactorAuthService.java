package com.wing.ecommercebackendwing.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthService {

    private final UserRepository userRepository;
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    @Transactional
    public String generateSecret(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        user.setTwofaSecret(key.getKey());
        userRepository.save(user);

        log.info("Generated 2FA secret for user: {}", userId);
        return key.getKey();
    }

    public String generateQRCodeUrl(String secret, String userEmail) {
        com.warrenstrange.googleauth.GoogleAuthenticatorKey key = 
            new com.warrenstrange.googleauth.GoogleAuthenticatorKey.Builder(secret).build();
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                "Wing-Ecommerce",
                userEmail,
                key
        );
    }

    @Transactional
    public boolean enable2FA(UUID userId, int verificationCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getTwofaSecret() == null) {
            throw new RuntimeException("2FA secret not generated. Please generate secret first.");
        }

        boolean isValid = googleAuthenticator.authorize(user.getTwofaSecret(), verificationCode);
        
        if (isValid) {
            user.setTwofaEnabled(true);
            userRepository.save(user);
            log.info("2FA enabled for user: {}", userId);
            return true;
        }
        
        log.warn("Invalid 2FA code provided for user: {}", userId);
        return false;
    }

    @Transactional
    public void disable2FA(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTwofaEnabled(false);
        user.setTwofaSecret(null);
        userRepository.save(user);

        log.info("2FA disabled for user: {}", userId);
    }

    public boolean verify2FACode(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }

    public boolean isUser2FAEnabled(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getTwofaEnabled)
                .orElse(false);
    }
}
