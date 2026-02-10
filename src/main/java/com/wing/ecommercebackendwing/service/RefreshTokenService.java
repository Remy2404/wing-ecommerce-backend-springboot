package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.config.JwtProperties;
import com.wing.ecommercebackendwing.exception.custom.TokenRefreshException;
import com.wing.ecommercebackendwing.model.entity.RefreshToken;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.repository.RefreshTokenRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    public RefreshToken createRefreshToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(
                jwtProperties.getRefreshToken().getExpiration()
        ));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByTokenAndRevokedFalse(token);
    }
    
    public Optional<RefreshToken> findByTokenIncludingRevoked(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public Optional<RefreshToken> findLatestActiveByUserId(UUID userId) {
        return refreshTokenRepository.findTopByUserIdAndRevokedFalseOrderByCreatedAtDesc(userId);
    }

    public boolean isRecentlyRevoked(RefreshToken token) {
        if (!Boolean.TRUE.equals(token.getRevoked())) {
            return false;
        }
        Instant revokedAt = token.getRevokedAt();
        if (revokedAt == null) {
            return false;
        }
        return revokedAt.isAfter(Instant.now().minusMillis(
                jwtProperties.getRefreshToken().getReuseLeewayMs()
        ));
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token expired. Please login again");
        }
        return token;
    }

    @Transactional
    public void deleteByUserId(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(Instant.now());
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public int revokeIfActive(String token) {
        return refreshTokenRepository.revokeIfActive(token);
    }

    public boolean isRevoked(String token) {
        return refreshTokenRepository.findByToken(token)
                .map(RefreshToken::getRevoked)
                .orElse(true);
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
