package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.repository.DeliveryRepository;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Object assignDriver(UUID deliveryId, Object assignRequest) {
        // TODO: Assign driver to delivery
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public void updateDeliveryStatus(UUID deliveryId, Object updateRequest) {
        // TODO: Update delivery status
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
