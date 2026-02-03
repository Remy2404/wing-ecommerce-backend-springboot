package com.wing.ecommercebackendwing.util;

import com.wing.ecommercebackendwing.model.entity.Order;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNumberGenerator {

    private final OrderRepository orderRepository;

    public String generateOrderNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ORD-" + datePrefix + "-";
        
        Optional<Order> lastOrder = orderRepository.findFirstByOrderNumberStartingWithOrderByOrderNumberDesc(prefix);
        
        int nextSequence = 1;
        if (lastOrder.isPresent()) {
            String lastOrderNumber = lastOrder.get().getOrderNumber();
            try {
                String sequencePart = lastOrderNumber.substring(lastOrderNumber.lastIndexOf("-") + 1);
                nextSequence = Integer.parseInt(sequencePart) + 1;
            } catch (Exception e) {
                log.warn("Failed to parse sequence from order number: {}", lastOrderNumber);
            }
        }
        
        return String.format("%s%03d", prefix, nextSequence);
    }
}
