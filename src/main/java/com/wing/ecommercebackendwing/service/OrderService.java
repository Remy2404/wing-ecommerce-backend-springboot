package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.OrderMapper;
import com.wing.ecommercebackendwing.dto.request.order.CreateOrderRequest;
import com.wing.ecommercebackendwing.dto.response.order.OrderResponse;
import com.wing.ecommercebackendwing.model.entity.Address;
import com.wing.ecommercebackendwing.model.entity.Cart;
import com.wing.ecommercebackendwing.model.entity.Merchant;
import com.wing.ecommercebackendwing.model.entity.Order;
import com.wing.ecommercebackendwing.model.entity.OrderItem;

import com.wing.ecommercebackendwing.model.enums.OrderStatus;
import com.wing.ecommercebackendwing.repository.AddressRepository;
import com.wing.ecommercebackendwing.repository.CartRepository;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.util.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final TaxService taxService;
    private final DeliveryFeeService deliveryFeeService;
    private final DiscountService discountService;

    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        // Fetch cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Verify ownership
        if (!cart.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        // Get merchant from first cart item (assuming all items from same merchant)
        Merchant merchant = cart.getItems().get(0).getProduct().getMerchant();
        if (merchant == null) {
            throw new RuntimeException("Product merchant not found");
        }

        // Create delivery address from shipping request
        Address deliveryAddress = new Address();
        deliveryAddress.setUser(cart.getUser());
        deliveryAddress.setStreet(request.getShippingAddress().getStreet());
        deliveryAddress.setCity(request.getShippingAddress().getCity());
        deliveryAddress.setProvince(request.getShippingAddress().getState());
        deliveryAddress.setPostalCode(request.getShippingAddress().getZipCode());
        deliveryAddress.setLabel("Order Delivery");
        deliveryAddress.setIsDefault(false);
        deliveryAddress.setCreatedAt(Instant.now());
        Address savedAddress = addressRepository.save(deliveryAddress);

        // Create order
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setMerchant(merchant);
        order.setDeliveryAddress(savedAddress);
        order.setOrderNumber(orderNumberGenerator.generateOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(Instant.now());
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        // Copy cart items to order items
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (com.wing.ecommercebackendwing.model.entity.CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setVariant(cartItem.getVariant());
            orderItem.setProductName(cartItem.getProduct().getName());
            orderItem.setProductImage(cartItem.getProduct().getImages());
            orderItem.setVariantName(cartItem.getVariant() != null ? cartItem.getVariant().getName() : null);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getPrice());
            
            BigDecimal itemSubtotal = cartItem.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
            orderItem.setSubtotal(itemSubtotal);
            orderItem.setCreatedAt(Instant.now());
            orderItem.setUpdatedAt(Instant.now());
            
            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(itemSubtotal);
        }

        // Calculate order totals using services
        BigDecimal subtotal = totalAmount;
        BigDecimal tax = taxService.calculateTax(subtotal);
        BigDecimal deliveryFee = deliveryFeeService.calculateFee(
                request.getDistanceKm() != null ? request.getDistanceKm() : BigDecimal.ZERO);
        BigDecimal discount = discountService.calculateDiscount(subtotal, request.getCouponCode());
        
        // Apply FREEDEL coupon
        if (discountService.isFreeDeliveryCoupon(request.getCouponCode())) {
            deliveryFee = BigDecimal.ZERO;
        }
        
        BigDecimal total = subtotal.add(deliveryFee).add(tax).subtract(discount);

        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setDeliveryFee(deliveryFee);
        order.setDiscount(discount);
        order.setTotal(total);
        order.setTotalAmount(total);

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Clear cart after successful order
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Created order {} for user {}", savedOrder.getOrderNumber(), userId);
        return OrderMapper.toResponse(savedOrder);
    }

    public Page<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "orderDate"));
        return orderRepository.findAll(pageable).map(OrderMapper::toResponse);
    }

    public Page<OrderResponse> getUserOrders(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "orderDate"));
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return orders.map(OrderMapper::toResponse);
    }

    public OrderResponse getOrderByNumber(UUID userId, String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Order number cannot be empty");
        }

        Order order = orderRepository.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new RuntimeException("Order not found"));

 // Verify ownership
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to order");
        }

        return OrderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus, UUID requestingUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Admin/delivery permission check should be done at controller level with @PreAuthorize

        order.setStatus(newStatus);
        order.setUpdatedAt(Instant.now());
        Order savedOrder = orderRepository.save(order);

        log.info("Updated order {} status to {} by user {}", orderId, newStatus, requestingUserId);
        return OrderMapper.toResponse(savedOrder);
    }

}
