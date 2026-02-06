package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.response.order.OrderResponse;
import com.wing.ecommercebackendwing.model.entity.Address;
import com.wing.ecommercebackendwing.repository.AddressRepository;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.ProductRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final RefreshTokenService refreshTokenService;

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

    public void revokeUser(java.util.UUID userId) {
        com.wing.ecommercebackendwing.model.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActive(false);
        userRepository.save(user);
        
        // Security: Revoke all refresh tokens
        refreshTokenService.revokeAllUserTokens(userId);
    }

    @Transactional
    public Map<String, Object> archiveLegacyOrderDeliveryAddresses() {
        List<Address> legacy = new ArrayList<>(addressRepository.findOrderLinkedAddressesByLabel("Order Delivery"));
        Instant now = Instant.now();

        for (Address address : legacy) {
            address.setLabel("ARCHIVED_ORDER_DELIVERY");
            address.setIsDefault(false);
            address.setUpdatedAt(now);
        }
        addressRepository.saveAll(legacy);

        Map<String, Object> result = new HashMap<>();
        result.put("archivedCount", legacy.size());
        result.put("archivedLabel", "ARCHIVED_ORDER_DELIVERY");
        result.put("timestamp", now);
        return result;
    }
}
