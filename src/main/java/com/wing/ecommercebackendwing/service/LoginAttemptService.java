package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public void loginSucceeded(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLastFailedLogin(null);
            user.setAccountLocked(false);
            user.setLockedUntil(null);
            userRepository.save(user);
        });
    }

    @Transactional
    public void loginFailed(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            user.setLastFailedLogin(Instant.now());

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                Instant lockUntil = Instant.now().plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES);
                user.setAccountLocked(true);
                user.setLockedUntil(lockUntil);
                
                log.warn("Account locked for user: {} until: {}", email, lockUntil);
                
                try {
                    emailService.sendAccountLockedEmail(email, lockUntil.toString());
                } catch (Exception e) {
                    log.error("Failed to send account locked email to: {}", email, e);
                }
            }

            userRepository.save(user);
        });
    }

    public boolean isBlocked(String email) {
        return userRepository.findByEmail(email)
                .map(this::isAccountLocked)
                .orElse(false);
    }

    private boolean isAccountLocked(User user) {
        if (!Boolean.TRUE.equals(user.getAccountLocked())) {
            return false;
        }

        if (user.getLockedUntil() != null && Instant.now().isAfter(user.getLockedUntil())) {
            // Auto-unlock
            unlockAccount(user.getId());
            return false;
        }

        return true;
    }

    @Transactional
    public void unlockAccount(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setAccountLocked(false);
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            log.info("Account unlocked for user: {}", userId);
        });
    }
}
