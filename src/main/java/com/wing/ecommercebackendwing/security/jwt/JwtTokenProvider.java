package com.wing.ecommercebackendwing.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import com.wing.ecommercebackendwing.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import com.wing.ecommercebackendwing.model.entity.User;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @jakarta.annotation.PostConstruct
    public void validateConfig() {
        String jwtSecret = jwtProperties.getSecret();
        if (jwtSecret == null || jwtSecret.length() < 32) {
            log.error("JWT Secret is missing or too short for HS256 algorithm. Minimum 32 characters required.");
            throw new IllegalStateException("Invalid JWT configuration");
        }
        log.info("JWT Signing Key successfully initialized from configuration.");
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Backwards-compatible entrypoint for access tokens.
     * Access tokens are JWTs; refresh tokens are opaque and handled by RefreshTokenService.
     */
    public String generateToken(Authentication authentication) {
        return generateAccessToken(authentication);
    }

    public String generateAccessToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessToken().getExpiration());

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(username)
                .setIssuer("wing-api")
                .setAudience("wing-client")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Backwards-compatible entrypoint for access tokens.
     * Access tokens are JWTs; refresh tokens are opaque and handled by RefreshTokenService.
     */
    public String generateToken(User user) {
        return generateAccessToken(user);
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessToken().getExpiration());

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(user.getEmail())
                .setIssuer("wing-api")
                .setAudience("wing-client")
                .claim("role", user.getRole().name())
                .claim("name", user.getFirstName() + " " + user.getLastName())
                .claim("avatar", user.getAvatar())
                .claim("id", user.getId())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer("wing-api")
                .requireAudience("wing-client")
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .requireIssuer("wing-api")
                    .requireAudience("wing-client")
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException ex) {
            log.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extract the JTI (token ID) from a JWT token.
     */
    public String getJtiFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getId();
        } catch (Exception e) {
            log.warn("Failed to extract JTI from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate a temporary token for 2FA verification (5 minutes expiry).
     * Token contains userId and type claim for validation.
     */
    public String generateTempToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 300000); // 5 minutes
        
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("type", "2FA_TEMP")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extract userId from temporary token with validation.
     * Validates token type and expiration.
     * 
     * @throws RuntimeException with specific error codes:
     *   - TEMP_TOKEN_EXPIRED: Token has expired
     *   - INVALID_TOKEN_TYPE: Token type is not 2FA_TEMP
     *   - INVALID_TEMP_TOKEN: Token is malformed or invalid
     */
    public UUID getUserIdFromTempToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            if (!"2FA_TEMP".equals(claims.get("type"))) {
                throw new RuntimeException("INVALID_TOKEN_TYPE");
            }
            
            return UUID.fromString(claims.getSubject());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new RuntimeException("TEMP_TOKEN_EXPIRED");
        } catch (RuntimeException e) {
            // Re-throw our custom exceptions
            throw e;
        } catch (Exception e) {
            log.error("Invalid temp token: {}", e.getMessage());
            throw new RuntimeException("INVALID_TEMP_TOKEN");
        }
    }

    /**
     * Refresh tokens are opaque strings stored in DB. This TTL is exposed for diagnostics/metrics if needed.
     */
    public long getRefreshTokenExpirationInMs() {
        return jwtProperties.getRefreshToken().getExpiration();
    }
}

