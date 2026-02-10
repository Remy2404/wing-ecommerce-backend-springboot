package com.wing.ecommercebackendwing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wing.ecommercebackendwing.controller.PaymentController;
import com.wing.ecommercebackendwing.security.jwt.JwtAuthenticationEntryPoint;
import com.wing.ecommercebackendwing.security.jwt.JwtAuthenticationFilter;
import com.wing.ecommercebackendwing.security.jwt.JwtTokenProvider;
import com.wing.ecommercebackendwing.security.jwt.TokenBlacklistService;
import com.wing.ecommercebackendwing.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@Import({
        SecurityConfig.class,
        PaymentVerifySecurityIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:3000")
class PaymentVerifySecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void verifyPayment_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/payments/verify/md5/test-md5"))
                .andExpect(status().isUnauthorized());
    }

    static class TestConfig {
        @Bean
        @Primary
        PaymentService paymentService() {
            return mock(PaymentService.class);
        }

        @Bean
        @Primary
        JwtTokenProvider jwtTokenProvider() {
            JwtTokenProvider provider = mock(JwtTokenProvider.class);
            when(provider.validateToken(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
            return provider;
        }

        @Bean
        @Primary
        UserDetailsService userDetailsService() {
            return mock(UserDetailsService.class);
        }

        @Bean
        @Primary
        TokenBlacklistService tokenBlacklistService() {
            return mock(TokenBlacklistService.class);
        }

        @Bean
        @Primary
        JwtAuthenticationFilter jwtAuthenticationFilter(
                JwtTokenProvider tokenProvider,
                UserDetailsService userDetailsService,
                TokenBlacklistService tokenBlacklistService
        ) {
            return new JwtAuthenticationFilter(tokenProvider, userDetailsService, tokenBlacklistService);
        }

        @Bean
        @Primary
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
            return new JwtAuthenticationEntryPoint(new ObjectMapper());
        }
    }
}
