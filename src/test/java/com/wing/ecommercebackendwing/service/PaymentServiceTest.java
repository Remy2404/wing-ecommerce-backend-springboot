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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(bakongConfig.getApiBaseUrl()).thenReturn("https://api.bakong.org/");
    }

    @Test
    void verifyPaymentByMd5_ShouldExtractTransactionId() {
        // Arrange
        Payment payment = new Payment();
        payment.setMd5(md5);
        payment.setAmount(new BigDecimal("12.50"));
        payment.setCurrency("USD");
        Order order = new Order();
        com.wing.ecommercebackendwing.model.entity.User user = new com.wing.ecommercebackendwing.model.entity.User();
        user.setId(userId);
        order.setUser(user);
        payment.setOrder(order);
        
        when(paymentRepository.findByMd5(anyString())).thenReturn(Optional.of(payment));
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("responseCode", "0");
        responseBody.put("responseMessage", "Success");
        
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", bakongTransactionId);
        data.put("amount", "12.50");
        data.put("currency", "USD");
        responseBody.put("data", data);
        
        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        // Act
        PaymentVerificationResponse result = paymentService.verifyPaymentByMd5(md5, userId);

        // Assert
        assertTrue(result.isPaid());
        assertEquals("Success", result.getMessage());
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals(expectedTransactionId, payment.getTransactionId());
        verify(paymentRepository, times(2)).save(payment);
    }

    @Test
    void verifyPaymentByMd5_ShouldSaveTransactionIdAsString() {
        // Arrange
        Payment payment = new Payment();
        payment.setMd5(md5);
        payment.setAmount(new BigDecimal("12.50"));
        payment.setCurrency("USD");
        Order order = new Order();
        com.wing.ecommercebackendwing.model.entity.User user = new com.wing.ecommercebackendwing.model.entity.User();
        user.setId(userId);
        order.setUser(user);
        payment.setOrder(order);
        
        when(paymentRepository.findByMd5(anyString())).thenReturn(Optional.of(payment));
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("responseCode", "0");
        responseBody.put("responseMessage", "Success");
        
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", "any-string-id");
        data.put("amount", "12.50");
        data.put("currency", "USD");
        responseBody.put("data", data);
        
        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        // Act
        PaymentVerificationResponse result = paymentService.verifyPaymentByMd5(md5, userId);

        // Assert
        assertTrue(result.isPaid());
        assertEquals("any-string-id", payment.getTransactionId());
        verify(paymentRepository, times(2)).save(payment);
    }

    @SuppressWarnings("rawtypes")
    @Test
    void verifyPaymentByMd5_ShouldNotComplete_WhenAmountMismatch() {
        Payment payment = new Payment();
        payment.setMd5(md5);
        payment.setAmount(new BigDecimal("12.50"));
        payment.setCurrency("USD");
        Order order = new Order();
        com.wing.ecommercebackendwing.model.entity.User user = new com.wing.ecommercebackendwing.model.entity.User();
        user.setId(userId);
        order.setUser(user);
        payment.setOrder(order);

        when(paymentRepository.findByMd5(anyString())).thenReturn(Optional.of(payment));

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("responseCode", "0");
        responseBody.put("responseMessage", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", "tx-1");
        data.put("amount", "99.99");
        data.put("currency", "USD");
        responseBody.put("data", data);

        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn((ResponseEntity) responseEntity);

        PaymentVerificationResponse result = paymentService.verifyPaymentByMd5(md5, userId);

        assertFalse(result.isPaid());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(paymentRepository, times(1)).save(payment);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @SuppressWarnings("rawtypes")
    @Test
    void verifyPaymentByMd5_ShouldNotComplete_WhenCurrencyMismatch() {
        Payment payment = new Payment();
        payment.setMd5(md5);
        payment.setAmount(new BigDecimal("12.50"));
        payment.setCurrency("USD");
        Order order = new Order();
        com.wing.ecommercebackendwing.model.entity.User user = new com.wing.ecommercebackendwing.model.entity.User();
        user.setId(userId);
        order.setUser(user);
        payment.setOrder(order);

        when(paymentRepository.findByMd5(anyString())).thenReturn(Optional.of(payment));

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("responseCode", "0");
        responseBody.put("responseMessage", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", "tx-1");
        data.put("amount", "12.50");
        data.put("currency", "KHR");
        responseBody.put("data", data);

        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn((ResponseEntity) responseEntity);

        PaymentVerificationResponse result = paymentService.verifyPaymentByMd5(md5, userId);

        assertFalse(result.isPaid());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(paymentRepository, times(1)).save(payment);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void verifyPaymentByMd5_WhenAlreadyCompleted_ShouldShortCircuitWithoutGatewayOrWrites() {
        Payment payment = new Payment();
        payment.setMd5(md5);
        payment.setAmount(new BigDecimal("12.50"));
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(java.time.Instant.now());
        payment.setTransactionId("tx-final");
        Order order = new Order();
        com.wing.ecommercebackendwing.model.entity.User user = new com.wing.ecommercebackendwing.model.entity.User();
        user.setId(userId);
        order.setUser(user);
        order.setStatus(com.wing.ecommercebackendwing.model.enums.OrderStatus.PAID);
        payment.setOrder(order);

        when(paymentRepository.findByMd5(md5)).thenReturn(Optional.of(payment));

        PaymentVerificationResponse result = paymentService.verifyPaymentByMd5(md5, userId);

        assertTrue(result.isPaid());
        assertEquals("Payment already completed", result.getMessage());
        assertEquals(12.50, result.getPaidAmount());
        assertEquals("USD", result.getCurrency());
        verify(restTemplate, never()).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @SuppressWarnings("rawtypes")
    @Test
    void verifyPaymentByMd5_Twice_ShouldBeIdempotentAndSecondCallHasNoSideEffects() {
        Payment payment = new Payment();
        payment.setMd5(md5);
        payment.setAmount(new BigDecimal("12.50"));
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.PENDING);
        Order order = new Order();
        com.wing.ecommercebackendwing.model.entity.User user = new com.wing.ecommercebackendwing.model.entity.User();
        user.setId(userId);
        order.setUser(user);
        order.setStatus(com.wing.ecommercebackendwing.model.enums.OrderStatus.PENDING);
        payment.setOrder(order);

        when(paymentRepository.findByMd5(anyString())).thenReturn(Optional.of(payment));

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("responseCode", "0");
        responseBody.put("responseMessage", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("externalRef", "ext-1001");
        data.put("amount", "12.50");
        data.put("currency", "USD");
        responseBody.put("data", data);

        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        PaymentVerificationResponse first = paymentService.verifyPaymentByMd5(md5, userId);
        PaymentVerificationResponse second = paymentService.verifyPaymentByMd5(md5, userId);

        assertTrue(first.isPaid());
        assertTrue(second.isPaid());
        assertEquals("Payment already completed", second.getMessage());
        assertEquals("ext-1001", payment.getTransactionId());
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class));
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @SuppressWarnings("rawtypes")
    @Test
    void verifyPaymentByMd5_WhenOrderAlreadyPaid_ShouldNotReapplyOrderUpdate() {
        Payment payment = new Payment();
        payment.setMd5(md5);
        payment.setAmount(new BigDecimal("12.50"));
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.PENDING);
        Order order = new Order();
        com.wing.ecommercebackendwing.model.entity.User user = new com.wing.ecommercebackendwing.model.entity.User();
        user.setId(userId);
        order.setUser(user);
        order.setStatus(com.wing.ecommercebackendwing.model.enums.OrderStatus.PAID);
        payment.setOrder(order);

        when(paymentRepository.findByMd5(anyString())).thenReturn(Optional.of(payment));

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("responseCode", "0");
        responseBody.put("responseMessage", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("instructionRef", "inst-1001");
        data.put("amount", "12.50");
        data.put("currency", "USD");
        responseBody.put("data", data);

        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        PaymentVerificationResponse result = paymentService.verifyPaymentByMd5(md5, userId);

        assertTrue(result.isPaid());
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
    }
}
