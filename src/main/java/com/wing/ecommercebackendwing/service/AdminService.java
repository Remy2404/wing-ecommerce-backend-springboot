package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.response.order.OrderResponse;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.ProductRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalOrders", orderRepository.count());
        stats.put("totalProducts", productRepository.count());
        stats.put("totalRevenue", orderRepository.sumTotalAmount()); // Need to add this to repository
        stats.put("timestamp", Instant.now());
        return stats;
    }

    public Page<OrderResponse> getAllOrders(int page, int size) {
        return orderService.getAllOrders(page, size);
    }
}
