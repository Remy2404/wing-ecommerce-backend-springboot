package com.wing.ecommercebackendwing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "khqr")
@Data
public class BakongConfig {
    private String apiBaseUrl;
    private String apiToken;
    private Merchant merchant;

    @Data
    public static class Merchant {
        private String bakongId;
        private String id;
        private String name;
        private String acquiringBank;
    }
}
