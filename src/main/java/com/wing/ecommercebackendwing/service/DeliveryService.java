package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.model.entity.Delivery;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.model.enums.DeliveryStatus;
import com.wing.ecommercebackendwing.repository.DeliveryRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;

    @Transactional
    public Delivery assignDriver(UUID deliveryId, UUID driverId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
        
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        delivery.setDriver(driver);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setUpdatedAt(Instant.now());
        
        return deliveryRepository.save(delivery);
    }

    @Transactional
    public void updateDeliveryStatus(UUID deliveryId, DeliveryStatus status, String notes) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        delivery.setStatus(status);
        if (notes != null) {
            delivery.setDriverNotes(notes);
        }

        if (status == DeliveryStatus.PICKED_UP) {
            delivery.setPickupTime(Instant.now());
        } else if (status == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredTime(Instant.now());
        }

        delivery.setUpdatedAt(Instant.now());
        deliveryRepository.save(delivery);
    }
}
