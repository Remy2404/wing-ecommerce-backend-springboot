package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.config.BakongConfig;
import com.wing.ecommercebackendwing.dto.response.payment.PaymentVerificationResponse;
import com.wing.ecommercebackendwing.model.entity.Order;
import com.wing.ecommercebackendwing.model.entity.Payment;
import com.wing.ecommercebackendwing.model.enums.PaymentStatus;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private BakongConfig bakongConfig;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private final String md5 = "aaad6ba89e1045b3ba46fa8542555882";
    private final String bakongTransactionId = "60241686f7708a4a520a778844556677"; // 32 chars
    private final String expectedTransactionId = "60241686f7708a4a520a778844556677";

    @BeforeEach
    void setUp() {
        lenient().when(bakongConfig.getApiBaseUrl()).thenReturn("https://api.bakong.org/");
    }

    @Test
    void verifyPaymentByMd5_ShouldExtractTransactionId() {
        // Arrange
        Payment payment = new Payment();
        payment.setMd5(md5);
        payment.setOrder(new Order());
        
        when(paymentRepository.findByMd5(anyString())).thenReturn(Optional.of(payment));
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("responseCode", "0");
        responseBody.put("responseMessage", "Success");
        
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", bakongTransactionId);
        responseBody.put("data", data);
        
        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        // Act
        PaymentVerificationResponse result = paymentService.verifyPaymentByMd5(md5);

        // Assert
        assertTrue(result.isPaid());
        assertEquals("Success", result.getMessage());
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals(expectedTransactionId, payment.getTransactionId());
        verify(paymentRepository).save(payment);
    }

    @SuppressWarnings("rawtypes")
    @Test
    void verifyPaymentByMd5_ShouldSaveTransactionIdAsString() {
        // Arrange
        Payment payment = new Payment();
        payment.setMd5(md5);
        payment.setOrder(new Order());
        
        when(paymentRepository.findByMd5(anyString())).thenReturn(Optional.of(payment));
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("responseCode", "0");
        responseBody.put("responseMessage", "Success");
        
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", "any-string-id");
        responseBody.put("data", data);
        
        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn((ResponseEntity) responseEntity);

        // Act
        PaymentVerificationResponse result = paymentService.verifyPaymentByMd5(md5);

        // Assert
        assertTrue(result.isPaid());
        assertEquals("any-string-id", payment.getTransactionId());
        verify(paymentRepository).save(payment);
    }
}
