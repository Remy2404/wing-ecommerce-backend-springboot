package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.response.auth.AuthResponse;
import com.wing.ecommercebackendwing.exception.custom.TokenRefreshException;
import com.wing.ecommercebackendwing.model.entity.RefreshToken;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.model.enums.UserRole;
import com.wing.ecommercebackendwing.repository.UserRepository;
import com.wing.ecommercebackendwing.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnhancedAuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private EmailService emailService;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private TwoFactorAuthService twoFactorAuthService;

    @InjectMocks
    private EnhancedAuthService enhancedAuthService;

    @Test
    void refreshToken_ShouldRotateOnce_WhenTokenIsActive() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("u@example.com");
        user.setFirstName("U");
        user.setLastName("S");
        user.setRole(UserRole.CUSTOMER);

        RefreshToken existing = new RefreshToken();
        existing.setToken("old-refresh");
        existing.setUser(user);

        RefreshToken rotated = new RefreshToken();
        rotated.setToken("new-refresh");
        rotated.setUser(user);

        when(refreshTokenService.findByTokenIncludingRevoked("old-refresh")).thenReturn(Optional.of(existing));
        when(refreshTokenService.verifyExpiration(existing)).thenReturn(existing);
        when(refreshTokenService.revokeIfActive("old-refresh")).thenReturn(1);
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(rotated);
        when(jwtTokenProvider.generateToken(user)).thenReturn("new-access");

        AuthResponse response = enhancedAuthService.refreshToken("old-refresh");

        assertNotNull(response);
        assertEquals("new-access", response.getToken());
        assertEquals("new-refresh", response.getRefreshToken());
        verify(refreshTokenService).revokeIfActive("old-refresh");
    }

    @Test
    void refreshToken_ShouldReject_WhenTokenAlreadyUsed() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("u@example.com");
        user.setFirstName("U");
        user.setLastName("S");
        user.setRole(UserRole.CUSTOMER);

        RefreshToken existing = new RefreshToken();
        existing.setToken("old-refresh");
        existing.setUser(user);

        when(refreshTokenService.findByTokenIncludingRevoked("old-refresh")).thenReturn(Optional.of(existing));
        when(refreshTokenService.verifyExpiration(existing)).thenReturn(existing);
        when(refreshTokenService.revokeIfActive("old-refresh")).thenReturn(0);

        assertThrows(TokenRefreshException.class, () -> enhancedAuthService.refreshToken("old-refresh"));

        verify(refreshTokenService, never()).createRefreshToken(user.getId());
        verify(jwtTokenProvider, never()).generateToken(user);
    }
}
