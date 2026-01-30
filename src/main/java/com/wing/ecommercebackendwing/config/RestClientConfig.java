package com.wing.ecommercebackendwing.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

@Configuration
@Slf4j
public class RestClientConfig {

    @Bean
    @Profile("dev")
    @SuppressWarnings("deprecation")
    public RestTemplate restTemplateDev() {
        log.warn("DEVELOPMENT PROFILE ACTIVE - Configuring RestTemplate with SSL bypass for Bakong SIT");
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial((chain, authType) -> true)
                    .build();

            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    NoopHostnameVerifier.INSTANCE
            );

            HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();

            HttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build();

            return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        } catch (Exception e) {
            log.error("Failed to configure SSL bypass, falling back to default RestTemplate", e);
            return new RestTemplate();
        }
    }

    @Bean
    @Profile("prod")
    public RestTemplate restTemplateProd() {
        log.info("PRODUCTION PROFILE ACTIVE - Using standard RestTemplate with SSL validation");
        return new RestTemplate();
    }
}
