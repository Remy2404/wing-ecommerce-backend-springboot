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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;

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
            // Detect token type: ID tokens have 3 parts (JWT), access tokens are opaque
            String[] parts = token.split("\\.");
            
            if (parts.length == 3) {
                // This looks like a JWT (ID token) - verify with Google
                return authenticateWithIdToken(token);
            } else {
                // This is likely an access token - call userinfo API
                return authenticateWithAccessToken(token);
            }
        } catch (Exception e) {
            log.error("Google authentication failed: {}", e.getMessage(), e);
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }

    private AuthResponse authenticateWithIdToken(String idTokenString) throws Exception {
        // Verify Google ID token
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new RuntimeException("Invalid Google ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        // Extract user information
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");
        Boolean emailVerified = payload.getEmailVerified();

        return processGoogleUser(googleId, email, name, pictureUrl, emailVerified);
    }

    private AuthResponse authenticateWithAccessToken(String accessToken) throws Exception {
        // Call Google's userinfo API with the access token
        NetHttpTransport httpTransport = new NetHttpTransport();
        com.google.api.client.http.HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
        
        com.google.api.client.http.GenericUrl url = new com.google.api.client.http.GenericUrl("https://www.googleapis.com/oauth2/v3/userinfo");
        com.google.api.client.http.HttpRequest request = requestFactory.buildGetRequest(url);
        request.getHeaders().setAuthorization("Bearer " + accessToken);
        
        com.google.api.client.http.HttpResponse response = request.execute();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> json = GsonFactory.getDefaultInstance().fromInputStream(response.getContent(), java.util.Map.class);
        
        String googleId = (String) json.get("sub");
        String email = (String) json.get("email");
        String name = (String) json.get("name");
        String pictureUrl = (String) json.get("picture");
        Boolean emailVerified = (Boolean) json.get("email_verified");
        
        if (googleId == null || email == null) {
            throw new RuntimeException("Failed to get user info from Google");
        }

        return processGoogleUser(googleId, email, name, pictureUrl, emailVerified);
    }

    private AuthResponse processGoogleUser(String googleId, String email, String name, String pictureUrl, Boolean emailVerified) {
        // Find or create user
        User user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> createGoogleUser(googleId, email, name, pictureUrl, emailVerified));

        // Update existing user with Google ID if logging in via Google for first time
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            user.setAuthProvider(AuthProvider.GOOGLE);
            if (emailVerified != null && emailVerified) {
                user.setEmailVerified(true);
            }
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

    private User createGoogleUser(String googleId, String email, String name, String pictureUrl, Boolean emailVerified) {
        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setEmailVerified(emailVerified != null && emailVerified);
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

    private Authentication createAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_" + user.getRole().name())
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
