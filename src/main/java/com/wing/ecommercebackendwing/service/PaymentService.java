package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Object processPayment(Object processRequest) {
        // TODO: Process payment with gateway
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public boolean verifyPayment(String transactionId) {
        // TODO: Verify payment status
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
