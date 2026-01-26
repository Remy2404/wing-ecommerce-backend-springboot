package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.request.order.CreateOrderRequest;
import com.wing.ecommercebackendwing.dto.response.order.OrderResponse;
import com.wing.ecommercebackendwing.repository.OrderItemRepository;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        // TODO: Create order from cart and payment
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<OrderResponse> getUserOrders(UUID userId) {
        // TODO: Get user's orders
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        // TODO: Find order by number
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public void updateStatus(UUID orderId, Object updateRequest) {
        // TODO: Update order status
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
