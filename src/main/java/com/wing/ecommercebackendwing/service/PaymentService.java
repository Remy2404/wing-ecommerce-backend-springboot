package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.config.BakongConfig;
import com.wing.ecommercebackendwing.model.entity.Order;
import com.wing.ecommercebackendwing.model.entity.Payment;
import com.wing.ecommercebackendwing.model.enums.OrderStatus;
import com.wing.ecommercebackendwing.model.enums.PaymentMethod;
import com.wing.ecommercebackendwing.model.enums.PaymentStatus;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.PaymentRepository;
import kh.org.nbc.bakong_khqr.BakongKHQR;
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

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final BakongConfig bakongConfig;
    private final RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${khqr.api-token}")
    private String bakongApiToken;

    @Transactional
    public com.wing.ecommercebackendwing.dto.response.payment.KHQRResponse generateKHQR(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is not in PENDING status");
        }

        MerchantInfo merchantInfo = new MerchantInfo();
        merchantInfo.setBakongAccountId(bakongConfig.getMerchant().getBakongId());
        merchantInfo.setMerchantId(bakongConfig.getMerchant().getId());
        merchantInfo.setMerchantName(bakongConfig.getMerchant().getName());
        merchantInfo.setAcquiringBank(bakongConfig.getMerchant().getAcquiringBank());
        merchantInfo.setCurrency(kh.org.nbc.bakong_khqr.model.KHQRCurrency.USD); 
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
        paymentRepository.save(payment);

        log.info("Generated KHQR for order {} with MD5 {}", order.getOrderNumber(), md5);

        return com.wing.ecommercebackendwing.dto.response.payment.KHQRResponse.builder()
                .qrData(qrData)
                .md5(md5)
                .orderNumber(order.getOrderNumber())
                .amount(order.getTotalAmount().toString())
                .build();
    }


    @Transactional
    public String verifyPaymentByMd5(String md5) {
        // Normalize MD5 to lowercase as some SDKs/APIs might vary in casing
        String normalizedMd5 = md5.trim().toLowerCase();
        
        Payment payment = paymentRepository.findByMd5(normalizedMd5)
                .orElse(null);

        if (payment == null) {
            log.warn("Payment record not found for MD5: {}", normalizedMd5);
            return "ERROR: Payment record not found for MD5 " + normalizedMd5;
        }

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return "SUCCESS: Payment already completed";
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


    private String executeVerificationRequest(String url, HttpEntity<Map<String, String>> entity, String transactionId, Payment payment) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Object responseCodeObj = responseBody.get("responseCode");
                Object responseMessageObj = responseBody.get("responseMessage");
                
                if (responseCodeObj != null && "0".equals(String.valueOf(responseCodeObj))) {
                    payment.setStatus(PaymentStatus.COMPLETED);
                    payment.setPaidAt(Instant.now());
                    payment.setGatewayResponse(responseBody.toString());
                    paymentRepository.save(payment);

                    Order order = payment.getOrder();
                    order.setStatus(OrderStatus.PAID);
                    order.setUpdatedAt(Instant.now());
                    orderRepository.save(order);

                    log.info("Payment verified successfully for transaction {}", transactionId);
                    return "SUCCESS: " + responseMessageObj;
                } else {
                    String errorMsg = responseMessageObj != null ? responseMessageObj.toString() : "Unknown error";
                    log.warn("Payment verification failed for transaction {}: {}", transactionId, errorMsg);
                    return "PENDING: " + errorMsg;
                }
            } else {
                return "ERROR: Bakong API returned status " + response.getStatusCode();
            }
        } catch (Exception e) {
            log.error("Error verifying payment for transaction {}: {}", transactionId, e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    @Transactional
    public String verifyPayment(String transactionId) {
        if (transactionId == null) {
            return "ERROR: Transaction ID is null";
        }
        
        return verifyPaymentByMd5(transactionId.trim());
    }
}
