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
        payment.setTransactionId(md5); 
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
    public boolean verifyPaymentByMd5(String md5) {
        Payment payment = paymentRepository.findByTransactionId(md5)
                .orElseThrow(() -> new RuntimeException("Payment record not found for MD5: " + md5));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return true;
        }

        String url = bakongConfig.getApiBaseUrl() + "/v1/check_transaction_by_md5";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bakongConfig.getApiToken());

        Map<String, String> requestBody = Map.of("md5", md5);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object responseCodeObj = response.getBody().get("responseCode");
                // Open API might return responseCode as String or Integer
                if (responseCodeObj != null && ("0".equals(responseCodeObj.toString()) || Integer.valueOf(0).equals(responseCodeObj))) {
                    // Success!
                    payment.setStatus(PaymentStatus.COMPLETED);
                    payment.setPaidAt(Instant.now());
                    payment.setGatewayResponse(response.getBody().toString());
                    paymentRepository.save(payment);

                    Order order = payment.getOrder();
                    order.setStatus(OrderStatus.PAID);
                    order.setUpdatedAt(Instant.now());
                    orderRepository.save(order);

                    log.info("Payment verified successfully for MD5 {}", md5);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Error verifying payment for MD5 {}: {}", md5, e.getMessage());
        }

        return false;
    }

    public boolean verifyPayment(String transactionId) {
        return verifyPaymentByMd5(transactionId);
    }
}
