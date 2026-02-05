package com.wing.ecommercebackendwing.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Validated
@Data
public class JwtProperties {

    @NotBlank
    private String secret;

    private AccessToken accessToken = new AccessToken();
    private RefreshToken refreshToken = new RefreshToken();

    @Data
    public static class AccessToken {
        @Min(1)
        private long expiration = 3600000;
    }

    @Data
    public static class RefreshToken {
        @Min(1)
        private long expiration = 2592000000L;

        @Min(0)
        private long reuseLeewayMs = 10000;
    }
}
