package com.wing.ecommercebackendwing.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.wing.ecommercebackendwing.dto.response.auth.AuthResponse;
import com.wing.ecommercebackendwing.model.entity.RefreshToken;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.model.enums.AuthProvider;
import com.wing.ecommercebackendwing.model.enums.UserRole;
import com.wing.ecommercebackendwing.repository.UserRepository;
import com.wing.ecommercebackendwing.security.jwt.JwtTokenProvider;
import com.wing.ecommercebackendwing.exception.custom.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Transactional
    public AuthResponse authenticateWithGoogle(String token) {
        try {
            // Try ID token first
            try {
                return authenticateWithIdToken(token);
            } catch (IllegalArgumentException e) {
                // Not a JWT, try as access token
                return authenticateWithAccessToken(token);
            } catch (Exception e) {
                log.error("ID token verification failed: {}", e.getMessage());
                // Fallback to access token if it wasn't a JWT error but a verification error
                // In a stricter setup, we might want to fail here if it looks like a JWT
                if (token.split("\\.").length != 3) {
                    return authenticateWithAccessToken(token);
                }
                throw new UnauthorizedException("Invalid Google authentication");
            }
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google authentication failed: ", e);
            throw new UnauthorizedException("Authentication failed");
        }
    }

    private AuthResponse authenticateWithIdToken(String idTokenString) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new UnauthorizedException("Invalid ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        Boolean emailVerified = payload.getEmailVerified();
        if (emailVerified == null || !emailVerified) {
            throw new UnauthorizedException("Google account email is not verified");
        }

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        return processGoogleUser(googleId, email, name, pictureUrl, emailVerified);
    }

    private AuthResponse authenticateWithAccessToken(String accessToken) throws Exception {
        // 1. Verify access token with tokeninfo endpoint
        NetHttpTransport httpTransport = new NetHttpTransport();
        com.google.api.client.http.HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
        
        com.google.api.client.http.GenericUrl tokenInfoUrl = new com.google.api.client.http.GenericUrl("https://oauth2.googleapis.com/tokeninfo?access_token=" + accessToken);
        com.google.api.client.http.HttpRequest tokenInfoRequest = requestFactory.buildGetRequest(tokenInfoUrl);
        
        com.google.api.client.http.HttpResponse tokenInfoResponse = tokenInfoRequest.execute();
        @SuppressWarnings("unchecked")
        Map<String, Object> tokenInfo = GsonFactory.getDefaultInstance().fromInputStream(tokenInfoResponse.getContent(), Map.class);
        
        // Validate azp (Authorized Party) should match our client ID
        String azp = (String) tokenInfo.get("azp");
        if (azp == null || !azp.equals(googleClientId)) {
            log.warn("Access token azp mismatch: expected {}, got {}", googleClientId, azp);
            throw new UnauthorizedException("Invalid token source");
        }

        // 2. Get user info
        com.google.api.client.http.GenericUrl userInfoUrl = new com.google.api.client.http.GenericUrl("https://www.googleapis.com/oauth2/v3/userinfo");
        com.google.api.client.http.HttpRequest userInfoRequest = requestFactory.buildGetRequest(userInfoUrl);
        userInfoRequest.getHeaders().setAuthorization("Bearer " + accessToken);
        
        com.google.api.client.http.HttpResponse userInfoResponse = userInfoRequest.execute();
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = GsonFactory.getDefaultInstance().fromInputStream(userInfoResponse.getContent(), Map.class);
        
        String googleId = (String) userInfo.get("sub");
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");
        String pictureUrl = (String) userInfo.get("picture");
        Boolean emailVerified = (Boolean) userInfo.get("email_verified");
        
        if (googleId == null || email == null) {
            throw new UnauthorizedException("Failed to get user info from Google");
        }

        if (emailVerified == null || !emailVerified) {
            throw new UnauthorizedException("Google account email is not verified");
        }

        return processGoogleUser(googleId, email, name, pictureUrl, emailVerified);
    }

    private AuthResponse processGoogleUser(String googleId, String email, String name, String pictureUrl, Boolean emailVerified) {
        if (emailVerified == null || !emailVerified) {
            throw new UnauthorizedException("Google account email is not verified");
        }
        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> {
                    // Try to find by email
                    User existingUser = userRepository.findByEmail(email).orElse(null);
                    if (existingUser != null) {
                        if (existingUser.getEmailVerified()) {
                            log.warn("Account takeover attempt blocked for email: {}", email);
                            throw new UnauthorizedException("This email is already registered. Please login with your password.");
                        }
                        return existingUser;
                    }
                    // Create new user if not found
                    return createGoogleUser(googleId, email, name, pictureUrl, emailVerified);
                });

        // Link Google ID if not already linked (for existing unverified users)
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setEmailVerified(emailVerified);
            userRepository.save(user);
        }

        // Generate JWT tokens
        String accessToken = jwtTokenProvider.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("Google OAuth login successful for user: {}", email);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(buildUserSummary(user))
                .build();
    }

    private User createGoogleUser(String googleId, String email, String name, String pictureUrl, boolean emailVerified) {
        if (!emailVerified) {
            throw new IllegalArgumentException("Cannot create a user with an unverified email");
        }
        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setEmailVerified(emailVerified);
        user.setAvatar(pictureUrl);
        user.setRole(UserRole.CUSTOMER);
        user.setIsActive(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        // Parse name into first and last name
        if (name != null && !name.isEmpty()) {
            String[] nameParts = name.split(" ", 2);
            user.setFirstName(nameParts[0]);
            user.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        } else {
            user.setFirstName("User");
            user.setLastName("");
        }

        return userRepository.save(user);
    }

    @SuppressWarnings("unused")
    private Authentication createAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                AuthorityUtils.createAuthorityList("ROLE_" + user.getRole().name())
        );
    }

    private AuthResponse.UserSummary buildUserSummary(User user) {
        return AuthResponse.UserSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole().name())
                .avatar(user.getAvatar())
                .build();
    }
}
