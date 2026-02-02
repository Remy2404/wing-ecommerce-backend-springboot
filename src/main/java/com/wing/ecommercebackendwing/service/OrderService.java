package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.OrderMapper;
import com.wing.ecommercebackendwing.dto.request.order.CreateOrderRequest;
import com.wing.ecommercebackendwing.dto.response.order.OrderResponse;
import com.wing.ecommercebackendwing.model.entity.*;
import com.wing.ecommercebackendwing.model.enums.OrderStatus;
import com.wing.ecommercebackendwing.repository.*;
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

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final TaxService taxService;
    private final DeliveryFeeService deliveryFeeService;
    private final DiscountService discountService;

    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        Merchant merchant = null;

        // Try to fetch cart (optional now if items are provided in request)
        Cart cart = cartRepository.findByUserId(userId).orElse(null);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            // Processing items from request
            for (com.wing.ecommercebackendwing.dto.request.order.OrderItemRequest itemReq : request.getItems()) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

                ProductVariant variant = null;
                if (itemReq.getVariantId() != null) {
                    variant = productVariantRepository.findById(itemReq.getVariantId())
                            .orElseThrow(() -> new RuntimeException("Variant not found: " + itemReq.getVariantId()));
                }

                // Check stock
                int stock = (variant != null) ? variant.getStock() : product.getStockQuantity();
                if (stock < itemReq.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for product " + product.getName());
                }

                // Price
                BigDecimal price = (variant != null) ? variant.getPrice() : product.getPrice();

                // Merchant (from first product)
                if (merchant == null) {
                    merchant = product.getMerchant();
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setVariant(variant);
                orderItem.setProductName(product.getName());
                orderItem.setProductImage(product.getImages());
                orderItem.setVariantName(variant != null ? variant.getName() : null);
                orderItem.setQuantity(itemReq.getQuantity());
                orderItem.setUnitPrice(price);

                BigDecimal itemSubtotal = price.multiply(new BigDecimal(itemReq.getQuantity()));
                orderItem.setSubtotal(itemSubtotal);
                orderItem.setCreatedAt(Instant.now());
                orderItem.setUpdatedAt(Instant.now());

                orderItems.add(orderItem);
                totalAmount = totalAmount.add(itemSubtotal);
            }
        } else if (cart != null && !cart.getItems().isEmpty()) {
            // Fallback: Copy cart items to order items
            merchant = cart.getItems().get(0).getProduct().getMerchant();
            for (com.wing.ecommercebackendwing.model.entity.CartItem cartItem : cart.getItems()) {
                OrderItem orderItem = new OrderItem();
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

                orderItems.add(orderItem);
                totalAmount = totalAmount.add(itemSubtotal);
            }
        } else {
            throw new RuntimeException("No items provided and no cart found");
        }

        if (merchant == null) {
            throw new RuntimeException("Product merchant not found");
        }

        // Create delivery address from shipping request
        Address deliveryAddress = new Address();
        deliveryAddress.setUser(user);
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
        order.setUser(user);
        order.setMerchant(merchant);
        order.setDeliveryAddress(savedAddress);
        order.setOrderNumber(orderNumberGenerator.generateOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(Instant.now());
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        order.setItems(new ArrayList<>());

        // Link items to order and add to list
        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getItems().add(item);
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

        // Clear cart after successful order if it exists
        if (cart != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }

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
