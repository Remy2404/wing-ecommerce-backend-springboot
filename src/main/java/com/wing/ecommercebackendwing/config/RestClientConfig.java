package com.wing.ecommercebackendwing.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

@Configuration
@Slf4j
public class RestClientConfig {

    @Value("${khqr.api-base-url}")
    private String bakongApiUrl;

    @Bean
    @SuppressWarnings("deprecation") // SSL bypass for SIT environment only
    public RestTemplate restTemplate() {
        // Check if we're using SIT environment
        boolean isSitEnvironment = bakongApiUrl != null && bakongApiUrl.contains("sit-api-bakong");
        
        if (isSitEnvironment) {
            log.warn("DETECTED SIT ENVIRONMENT - Configuring RestTemplate with SSL bypass");
            log.warn("THIS CONFIGURATION SHOULD NEVER BE USED IN PRODUCTION!");
            
            try {
                // Create SSL context that trusts all certificates
                SSLContext sslContext = SSLContextBuilder.create()
                        .loadTrustMaterial((chain, authType) -> true) // Trust all certificates
                        .build();

                // Create SSL socket factory with NoopHostnameVerifier
                SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                        sslContext,
                        NoopHostnameVerifier.INSTANCE // Accept all hostnames
                );

                // Create connection manager with custom SSL factory
                HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(sslSocketFactory)
                        .build();

                // Create HTTP client with custom connection manager
                HttpClient httpClient = HttpClients.custom()
                        .setConnectionManager(connectionManager)
                        .build();

                // Create request factory with custom HTTP client
                HttpComponentsClientHttpRequestFactory requestFactory = 
                        new HttpComponentsClientHttpRequestFactory(httpClient);
                
                log.info("RestTemplate configured with SSL bypass for SIT environment");
                return new RestTemplate(requestFactory);
                
            } catch (Exception e) {
                log.error("Failed to configure SSL bypass, falling back to default RestTemplate", e);
                return new RestTemplate();
            }
        } else {
            log.info(" Production environment detected - using default RestTemplate with SSL validation");
            return new RestTemplate();
        }
    }
}
