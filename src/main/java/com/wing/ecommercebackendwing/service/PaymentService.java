package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.config.BakongConfig;
import com.wing.ecommercebackendwing.dto.response.payment.KHQRResultDto;
import com.wing.ecommercebackendwing.dto.response.payment.PaymentVerificationResponse;
import com.wing.ecommercebackendwing.model.entity.Order;
import com.wing.ecommercebackendwing.model.entity.Payment;
import com.wing.ecommercebackendwing.model.enums.OrderStatus;
import com.wing.ecommercebackendwing.model.enums.PaymentMethod;
import com.wing.ecommercebackendwing.model.enums.PaymentStatus;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.PaymentRepository;
import kh.org.nbc.bakong_khqr.BakongKHQR;
import kh.org.nbc.bakong_khqr.model.KHQRCurrency;
import kh.org.nbc.bakong_khqr.model.KHQRData;
import kh.org.nbc.bakong_khqr.model.MerchantInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private static final long MAX_KHQR_EXPIRATION_SECONDS = 10 * 60;

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final BakongConfig bakongConfig;
    private final RestTemplate restTemplate;


    @org.springframework.beans.factory.annotation.Value("${khqr.api-token}")
    private String bakongApiToken;

    @org.springframework.beans.factory.annotation.Value("${khqr.expiration-seconds:600}")
    private long khqrExpirationSeconds;

    @Transactional
    public KHQRResultDto generateKHQR(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: This order does not belong to you.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is not in PENDING status");
        }

        MerchantInfo merchantInfo = new MerchantInfo();
        merchantInfo.setBakongAccountId(bakongConfig.getMerchant().getBakongId());
        merchantInfo.setMerchantId(bakongConfig.getMerchant().getId());
        merchantInfo.setMerchantName(bakongConfig.getMerchant().getName());
        merchantInfo.setAcquiringBank(bakongConfig.getMerchant().getAcquiringBank());
        merchantInfo.setCurrency(KHQRCurrency.USD);
        merchantInfo.setAmount(order.getTotalAmount().doubleValue());

        kh.org.nbc.bakong_khqr.model.KHQRResponse<KHQRData> sdkResponse = BakongKHQR.generateMerchant(merchantInfo);

        // Check for errors - handle null errorCode gracefully
        if (sdkResponse.getKHQRStatus() != null) {
            Integer errorCode = sdkResponse.getKHQRStatus().getErrorCode();
            // Treat null errorCode as success (0)
            if (errorCode != null && errorCode != 0) {
                log.error("Failed to generate KHQR code. Error code: {}, Message: {}", 
                        errorCode, sdkResponse.getKHQRStatus().getMessage());
                throw new RuntimeException("Failed to generate KHQR code: " + sdkResponse.getKHQRStatus().getMessage());
            }
        }

        String qrData = sdkResponse.getData().getQr();
        String md5 = sdkResponse.getData().getMd5();

        // Save or update payment record
        Payment payment = order.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
            payment.setCreatedAt(Instant.now());
        }
        payment.setMethod(PaymentMethod.KHQR);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(order.getTotalAmount());
        payment.setMd5(md5);
        
        long ttlSeconds = Math.min(Math.max(khqrExpirationSeconds, 1), MAX_KHQR_EXPIRATION_SECONDS);
        payment.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        
        paymentRepository.save(payment);

        log.info("Generated KHQR for order {} with MD5 {}", order.getOrderNumber(), md5);

        return KHQRResultDto.builder()
                .qrString(qrData)
                .md5(md5)
                .expiresAt(payment.getExpiresAt())
                .build();
    }


    @Transactional
    public PaymentVerificationResponse verifyPaymentByMd5(String md5, UUID userId) {
        // Normalize MD5 to lowercase as some SDKs/APIs might vary in casing
        String normalizedMd5 = md5.trim().toLowerCase();
        
        Payment payment = paymentRepository.findByMd5(normalizedMd5)
                .orElse(null);

        if (payment == null) {
            log.warn("Payment record not found for MD5: {}", normalizedMd5);
            return PaymentVerificationResponse.builder()
                    .isPaid(false)
                    .message("Payment record not found")
                    .build();
        }

        // Verify ownership
        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: This payment does not belong to you.");
        }

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return PaymentVerificationResponse.builder()
                    .isPaid(true)
                    .expired(false)
                    .paidAmount(payment.getAmount().doubleValue())
                    .currency("USD")
                    .message("Payment already completed")
                    .build();
        }

        if (payment.getStatus() == PaymentStatus.EXPIRED) {
            return PaymentVerificationResponse.builder()
                    .isPaid(false)
                    .expired(true)
                    .message("Transaction timed out. Please generate a new QR code.")
                    .build();
        }

        // Check if QR code has expired
        if (payment.getExpiresAt() != null && Instant.now().isAfter(payment.getExpiresAt())) {
            log.warn("QR code expired for payment MD5: {}", normalizedMd5);
            payment.setStatus(PaymentStatus.EXPIRED);
            payment.setGatewayResponse("KHQR timeout reached");
            paymentRepository.save(payment);
            return PaymentVerificationResponse.builder()
                    .isPaid(false)
                    .expired(true)
                    .message("Transaction timed out. Please generate a new QR code.")
                    .build();
        }

        String baseUrl = bakongConfig.getApiBaseUrl();
        String url = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "v1/check_transaction_by_md5";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(bakongApiToken);
        
        log.info("Verifying payment with Bakong Open API for MD5: {}", normalizedMd5);

        Map<String, String> requestBody = Map.of("md5", normalizedMd5);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        return executeVerificationRequest(url, entity, normalizedMd5, payment);
    }


    private PaymentVerificationResponse executeVerificationRequest(String url, HttpEntity<Map<String, String>> entity, String transactionId, Payment payment) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Object responseCodeObj = responseBody.get("responseCode");
                Object responseMessageObj = responseBody.get("responseMessage");
                
                // responseCode 0 is success
                if (responseCodeObj != null && "0".equals(String.valueOf(responseCodeObj))) {
                    Object dataObj = responseBody.get("data");
                    Double amount = 0.0;
                    String currency = "USD";
                    
                    if (dataObj instanceof Map) {
                        Map<?, ?> data = (Map<?, ?>) dataObj;
                        Object resTransactionId = data.get("transactionId");
                        if (resTransactionId != null) {
                            payment.setTransactionId(String.valueOf(resTransactionId));
                        }
                        
                        Object amountObj = data.get("amount");
                        if (amountObj != null) {
                            amount = Double.valueOf(String.valueOf(amountObj));
                        }
                        
                        Object currencyObj = data.get("currency");
                        if (currencyObj != null) {
                            currency = String.valueOf(currencyObj);
                        }
                    }

                    double expectedAmount = payment.getAmount() != null ? payment.getAmount().doubleValue() : 0.0;
                    String expectedCurrency = payment.getCurrency() != null ? payment.getCurrency() : "USD";
                    if (Double.compare(amount, expectedAmount) != 0 ||
                            !expectedCurrency.equalsIgnoreCase(currency)) {
                        log.warn("Payment amount/currency mismatch for transaction {}. expectedAmount={}, actualAmount={}, expectedCurrency={}, actualCurrency={}",
                                transactionId, expectedAmount, amount, expectedCurrency, currency);
                        return PaymentVerificationResponse.builder()
                                .isPaid(false)
                                .expired(false)
                                .message("Payment verification failed due to amount or currency mismatch")
                                .build();
                    }

                    payment.setStatus(PaymentStatus.COMPLETED);
                    payment.setPaidAt(Instant.now());
                    payment.setGatewayResponse(responseBody.toString());
                    paymentRepository.save(payment);

                    Order order = payment.getOrder();
                    order.setStatus(OrderStatus.PAID);
                    order.setUpdatedAt(Instant.now());
                    orderRepository.save(order);

                    log.info("Payment verified successfully for transaction {}", transactionId);
                    
                    return PaymentVerificationResponse.builder()
                            .isPaid(true)
                            .expired(false)
                            .paidAmount(amount)
                            .currency(currency)
                            .message("Success")
                            .build();
                } else {
                    String errorMsg = responseMessageObj != null ? responseMessageObj.toString() : "Transaction pending or not found";
                    log.debug("Payment verification pending for transaction {}: {}", transactionId, errorMsg);
                    return PaymentVerificationResponse.builder()
                            .isPaid(false)
                            .expired(false)
                            .message(errorMsg)
                            .build();
                }
            } else {
                return PaymentVerificationResponse.builder()
                        .isPaid(false)
                        .expired(false)
                        .message("API Error: " + response.getStatusCode())
                        .build();
            }
        } catch (Exception e) {
            log.error("Error verifying payment for transaction {}: {}", transactionId, e.getMessage());
            return PaymentVerificationResponse.builder()
                    .isPaid(false)
                    .expired(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    @Transactional
    public PaymentVerificationResponse verifyPayment(String transactionId, UUID userId) {
        if (transactionId == null) {
            return PaymentVerificationResponse.builder()
                    .isPaid(false)
                    .expired(false)
                    .message("Transaction ID is null")
                    .build();
        }
        
        return verifyPaymentByMd5(transactionId.trim(), userId);
    }


}
