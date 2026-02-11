package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.mapper.OrderMapper;
import com.wing.ecommercebackendwing.dto.request.order.CreateOrderRequest;
import com.wing.ecommercebackendwing.dto.response.order.OrderResponse;
import com.wing.ecommercebackendwing.exception.custom.BadRequestException;
import com.wing.ecommercebackendwing.exception.custom.ForbiddenException;
import com.wing.ecommercebackendwing.exception.custom.ResourceNotFoundException;
import com.wing.ecommercebackendwing.model.entity.*;
import com.wing.ecommercebackendwing.model.enums.OrderStatus;
import com.wing.ecommercebackendwing.repository.*;
import com.wing.ecommercebackendwing.util.OrderNumberGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import java.util.*;
import com.wing.ecommercebackendwing.model.enums.UserRole;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.PAID),
            OrderStatus.PAID, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING, Set.of(OrderStatus.READY, OrderStatus.CANCELLED),
            OrderStatus.READY, Set.of(OrderStatus.DELIVERING, OrderStatus.CANCELLED),
            OrderStatus.DELIVERING, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Collections.emptySet(),
            OrderStatus.CANCELLED, Collections.emptySet()
    );

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
    private final WingPointsService wingPointsService;
    private final OrderIdempotencyRecordRepository orderIdempotencyRecordRepository;
    private final ObjectMapper objectMapper;
    private final PhoneNumberService phoneNumberService;

    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        return createOrder(userId, request, null);
    }

    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return OrderMapper.toResponse(createOrderInternal(userId, request));
        }

        String normalizedKey = idempotencyKey.trim();
        String requestHash = buildRequestHash(request);
        Optional<OrderIdempotencyRecord> existing = orderIdempotencyRecordRepository
                .findByUserIdAndIdempotencyKey(userId, normalizedKey);

        if (existing.isPresent()) {
            OrderIdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new BadRequestException("Idempotency-Key already used with different payload");
            }
            if (record.getOrder() != null) {
                return OrderMapper.toResponse(record.getOrder());
            }
            throw new BadRequestException("Request with this Idempotency-Key is already in progress");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Instant now = Instant.now();
        OrderIdempotencyRecord record = new OrderIdempotencyRecord();
        record.setUser(user);
        record.setIdempotencyKey(normalizedKey);
        record.setRequestHash(requestHash);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        orderIdempotencyRecordRepository.save(record);

        Order createdOrder = createOrderInternal(userId, request);
        record.setOrder(createdOrder);
        record.setUpdatedAt(Instant.now());
        orderIdempotencyRecordRepository.save(record);
        return OrderMapper.toResponse(createdOrder);
    }

    private Order createOrderInternal(UUID userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        Merchant merchant = null;
        Map<UUID, Product> lockedProducts = new HashMap<>();
        Map<UUID, ProductVariant> lockedVariants = new HashMap<>();
        Map<UUID, Integer> requestedProductStock = new HashMap<>();
        Map<UUID, Integer> requestedVariantStock = new HashMap<>();

        // Try to fetch cart (optional now if items are provided in request)
        Cart cart = cartRepository.findByUserId(userId).orElse(null);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            // Processing items from request (Buy Now flow)
            for (com.wing.ecommercebackendwing.dto.request.order.OrderItemRequest itemReq : request.getItems()) {
                int quantity = itemReq.getQuantity() != null ? itemReq.getQuantity() : 0;
                if (quantity <= 0) {
                    throw new BadRequestException("Item quantity must be at least 1");
                }

                Product product = lockedProducts.computeIfAbsent(
                        itemReq.getProductId(),
                        this::findProductForStockReservation
                );

                ProductVariant variant = null;
                if (itemReq.getVariantId() != null) {
                    UUID variantId = itemReq.getVariantId();
                    variant = lockedVariants.computeIfAbsent(
                            variantId,
                            this::findVariantForStockReservation
                    );
                    if (!variant.getProduct().getId().equals(product.getId())) {
                        throw new BadRequestException("Variant does not belong to the specified product");
                    }
                    requestedVariantStock.merge(variantId, quantity, Integer::sum);
                } else {
                    requestedProductStock.merge(product.getId(), quantity, Integer::sum);
                }

                // Price
                BigDecimal price = (variant != null) ? variant.getPrice() : product.getPrice();

                // Merchant Validation
                if (merchant == null) {
                    merchant = product.getMerchant();
                } else if (!merchant.getId().equals(product.getMerchant().getId())) {
                    throw new BadRequestException("All items in an order must belong to the same merchant");
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setVariant(variant);
                orderItem.setProductName(product.getName());
                orderItem.setProductImage(product.getImages());
                orderItem.setVariantName(variant != null ? variant.getName() : null);
                orderItem.setQuantity(quantity);
                orderItem.setUnitPrice(price);

                BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(quantity));
                orderItem.setSubtotal(itemSubtotal);
                orderItem.setCreatedAt(Instant.now());
                orderItem.setUpdatedAt(Instant.now());

                orderItems.add(orderItem);
                totalAmount = totalAmount.add(itemSubtotal);
            }
        } else if (cart != null && !cart.getItems().isEmpty()) {
            // Checkout from cart flow
            for (com.wing.ecommercebackendwing.model.entity.CartItem cartItem : cart.getItems()) {
                int quantity = cartItem.getQuantity() != null ? cartItem.getQuantity() : 0;
                if (quantity <= 0) {
                    throw new BadRequestException("Cart contains an invalid quantity for product");
                }

                Product product = lockedProducts.computeIfAbsent(
                        cartItem.getProduct().getId(),
                        id -> findProductForStockReservation(id, cartItem.getProduct())
                );
                ProductVariant variant = null;
                if (cartItem.getVariant() != null) {
                    UUID variantId = cartItem.getVariant().getId();
                    variant = lockedVariants.computeIfAbsent(
                            variantId,
                            id -> findVariantForStockReservation(id, cartItem.getVariant())
                    );
                    if (!variant.getProduct().getId().equals(product.getId())) {
                        throw new BadRequestException("Variant does not belong to the specified product");
                    }
                    requestedVariantStock.merge(variantId, quantity, Integer::sum);
                } else {
                    requestedProductStock.merge(product.getId(), quantity, Integer::sum);
                }

                // Merchant Validation
                if (merchant == null) {
                    merchant = product.getMerchant();
                } else if (!merchant.getId().equals(product.getMerchant().getId())) {
                    throw new BadRequestException("Your cart contains items from multiple merchants. Please checkout separately per merchant.");
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setVariant(variant);
                orderItem.setProductName(product.getName());
                orderItem.setProductImage(product.getImages());
                orderItem.setVariantName(variant != null ? variant.getName() : null);
                orderItem.setQuantity(quantity);
                BigDecimal unitPrice = cartItem.getPrice() != null
                        ? cartItem.getPrice()
                        : (variant != null ? variant.getPrice() : product.getPrice());
                orderItem.setUnitPrice(unitPrice);

                BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
                orderItem.setSubtotal(itemSubtotal);
                orderItem.setCreatedAt(Instant.now());
                orderItem.setUpdatedAt(Instant.now());

                orderItems.add(orderItem);
                totalAmount = totalAmount.add(itemSubtotal);
            }
        } else {
            throw new BadRequestException("No items provided and no cart found");
        }

        reserveLockedStock(lockedProducts, requestedProductStock, lockedVariants, requestedVariantStock);

        if (merchant == null) {
            throw new BadRequestException("Product merchant not found");
        }

        Address savedAddress;
        if (request.getShippingAddressId() != null) {
            Address existingAddress = addressRepository.findById(request.getShippingAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));
            if (!existingAddress.getUser().getId().equals(userId)) {
                throw new BadRequestException("Shipping address does not belong to the current user");
            }
            savedAddress = existingAddress;
        } else {
            if (request.getShippingAddress() == null) {
                throw new BadRequestException("Shipping address is required");
            }
            // Fallback for legacy clients that do not pass shippingAddressId.
            Address deliveryAddress = new Address();
            deliveryAddress.setUser(user);
            deliveryAddress.setLabel("Order Delivery");
            deliveryAddress.setFullName(request.getShippingAddress().getFullName());
            deliveryAddress.setPhone(phoneNumberService.normalizeToE164(
                    request.getShippingAddress().getPhone(),
                    request.getShippingAddress().getCountry()
            ));
            deliveryAddress.setStreet(request.getShippingAddress().getStreet());
            deliveryAddress.setCity(request.getShippingAddress().getCity());
            deliveryAddress.setProvince(request.getShippingAddress().getState());
            deliveryAddress.setCountry(request.getShippingAddress().getCountry());
            deliveryAddress.setPostalCode(request.getShippingAddress().getZipCode());
            deliveryAddress.setIsDefault(false);
            deliveryAddress.setCreatedAt(Instant.now());
            deliveryAddress.setUpdatedAt(Instant.now());
            savedAddress = addressRepository.save(deliveryAddress);
        }

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
        DiscountService.AppliedDiscount appliedDiscount =
                discountService.resolveDiscountForCheckout(subtotal, request.getCouponCode(), userId);
        if (appliedDiscount == null) {
            BigDecimal fallbackDiscount = discountService.calculateDiscount(subtotal, request.getCouponCode());
            appliedDiscount = new DiscountService.AppliedDiscount(
                    null,
                    fallbackDiscount != null ? fallbackDiscount : BigDecimal.ZERO,
                    request.getCouponCode()
            );
        }
        BigDecimal discount = appliedDiscount.amount();

        BigDecimal total = subtotal.add(deliveryFee).add(tax).subtract(discount);

        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setDeliveryFee(deliveryFee);
        order.setDiscount(discount);
        order.setTotal(total);
        order.setTotalAmount(total);

        // Save order
        Order savedOrder = orderRepository.save(order);
        discountService.recordPromotionUsage(appliedDiscount, userId, savedOrder);

        // Clear cart ONLY if we checked out FROM the cart
        if (request.getItems() == null || request.getItems().isEmpty()) {
            if (cart != null) {
                cart.getItems().clear();
                cartRepository.save(cart);
            }
        }

        log.info("Created order {} for user {}", savedOrder.getOrderNumber(), userId);
        return savedOrder;
    }

    private String buildRequestHash(CreateOrderRequest request) {
        try {
            String canonicalRequest = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new BadRequestException("Unable to process idempotent request");
        }
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

    public Page<OrderResponse> getMerchantOrders(UUID userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getMerchant() == null) {
            throw new RuntimeException("User is not a merchant");
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "orderDate"));
        Page<Order> orders = orderRepository.findByMerchantId(user.getMerchant().getId(), pageable);
        return orders.map(OrderMapper::toResponse);
    }

    public OrderResponse getOrderByNumber(UUID userId, String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Order number cannot be empty");
        }

        Order order = orderRepository.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        User requestingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify ownership/access: Buyer, Assigned Merchant, or Admin
        boolean isBuyer = order.getUser().getId().equals(userId);
        boolean isMerchant = order.getMerchant().getUser().getId().equals(userId);
        boolean isAdmin = requestingUser.getRole() == UserRole.ADMIN;

        if (!isBuyer && !isMerchant && !isAdmin) {
            throw new RuntimeException("Unauthorized access to order");
        }

        return OrderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus, UUID requestingUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        OrderStatus previousStatus = order.getStatus();

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        com.wing.ecommercebackendwing.model.enums.UserRole role = requestingUser.getRole();

        // Authorization Logic
        if (role == com.wing.ecommercebackendwing.model.enums.UserRole.ADMIN) {
            // Admin can do anything
        } else if (role == com.wing.ecommercebackendwing.model.enums.UserRole.MERCHANT) {
            // Merchant can only update their own orders
            if (!order.getMerchant().getUser().getId().equals(requestingUserId)) {
                throw new ForbiddenException("Unauthorized: This order does not belong to your store.");
            }
        } else {
            // Regular user can ONLY cancel their own order if it's still pending
            if (newStatus == OrderStatus.CANCELLED) {
                if (!order.getUser().getId().equals(requestingUserId)) {
                    throw new ForbiddenException("Unauthorized: You can only cancel your own orders.");
                }
                if (order.getStatus() != OrderStatus.PENDING) {
                    throw new BadRequestException("Cannot cancel order that is already " + order.getStatus());
                }
            } else {
                throw new ForbiddenException("Unauthorized: You do not have permission to update order status to " + newStatus);
            }
        }

        if (!isTransitionAllowed(order.getStatus(), newStatus)) {
            throw new BadRequestException("Illegal order status transition from " + order.getStatus() + " to " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELLED && previousStatus != OrderStatus.CANCELLED) {
            restoreStockForCancelledOrder(order);
            if (wingPointsService != null) {
                wingPointsService.revokeEarnedPointsForOrder(order.getUser().getId(), order.getId());
            }
        }

        order.setStatus(newStatus);
        
        // Update specific functional timestamps
        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(Instant.now());
        } else if (newStatus == OrderStatus.CANCELLED) {
            order.setCancelledAt(Instant.now());
        }
        
        order.setUpdatedAt(Instant.now());
        Order savedOrder = orderRepository.save(order);

        log.info("Updated order {} status to {} by user {}", orderId, newStatus, requestingUserId);
        return OrderMapper.toResponse(savedOrder);
    }

    private Product findProductForStockReservation(UUID productId) {
        return productRepository.findByIdForUpdate(productId)
                .or(() -> productRepository.findById(productId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }

    private Product findProductForStockReservation(UUID productId, Product fallbackProduct) {
        Optional<Product> resolved = productRepository.findByIdForUpdate(productId)
                .or(() -> productRepository.findById(productId));
        if (resolved.isPresent()) {
            return resolved.get();
        }
        if (fallbackProduct != null && productId.equals(fallbackProduct.getId())) {
            return fallbackProduct;
        }
        throw new ResourceNotFoundException("Product not found: " + productId);
    }

    private ProductVariant findVariantForStockReservation(UUID variantId) {
        return productVariantRepository.findByIdForUpdate(variantId)
                .or(() -> productVariantRepository.findById(variantId))
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));
    }

    private ProductVariant findVariantForStockReservation(UUID variantId, ProductVariant fallbackVariant) {
        Optional<ProductVariant> resolved = productVariantRepository.findByIdForUpdate(variantId)
                .or(() -> productVariantRepository.findById(variantId));
        if (resolved.isPresent()) {
            return resolved.get();
        }
        if (fallbackVariant != null && variantId.equals(fallbackVariant.getId())) {
            return fallbackVariant;
        }
        throw new ResourceNotFoundException("Variant not found: " + variantId);
    }

    private void reserveLockedStock(Map<UUID, Product> lockedProducts,
                                    Map<UUID, Integer> requestedProductStock,
                                    Map<UUID, ProductVariant> lockedVariants,
                                    Map<UUID, Integer> requestedVariantStock) {
        for (Map.Entry<UUID, Integer> entry : requestedVariantStock.entrySet()) {
            UUID variantId = entry.getKey();
            int requested = entry.getValue();
            ProductVariant variant = lockedVariants.get(variantId);
            int available = variant != null && variant.getStock() != null ? variant.getStock() : 0;
            if (variant == null || available < requested) {
                String productName = (variant != null && variant.getProduct() != null)
                        ? variant.getProduct().getName()
                        : "selected variant";
                throw new BadRequestException("Insufficient stock for product " + productName);
            }
        }

        for (Map.Entry<UUID, Integer> entry : requestedProductStock.entrySet()) {
            UUID productId = entry.getKey();
            int requested = entry.getValue();
            Product product = lockedProducts.get(productId);
            int available = product != null && product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (product == null || available < requested) {
                String productName = product != null ? product.getName() : "selected product";
                throw new BadRequestException("Insufficient stock for product " + productName);
            }
        }

        for (Map.Entry<UUID, Integer> entry : requestedVariantStock.entrySet()) {
            int updatedRows = productVariantRepository.decrementStockIfAvailable(entry.getKey(), entry.getValue());
            if (updatedRows == 0) {
                ProductVariant variant = lockedVariants.get(entry.getKey());
                String productName = (variant != null && variant.getProduct() != null)
                        ? variant.getProduct().getName()
                        : "selected variant";
                throw new BadRequestException("Insufficient stock for product " + productName);
            }
        }

        for (Map.Entry<UUID, Integer> entry : requestedProductStock.entrySet()) {
            int updatedRows = productRepository.decrementStockIfAvailable(entry.getKey(), entry.getValue());
            if (updatedRows == 0) {
                Product product = lockedProducts.get(entry.getKey());
                String productName = product != null ? product.getName() : "selected product";
                throw new BadRequestException("Insufficient stock for product " + productName);
            }
        }
    }

    private boolean isTransitionAllowed(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return true;
        }
        Set<OrderStatus> allowed = ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
        return allowed.contains(newStatus);
    }

    private void restoreStockForCancelledOrder(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }

        for (OrderItem item : order.getItems()) {
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
            if (quantity <= 0) {
                continue;
            }

            if (item.getVariant() != null) {
                productVariantRepository.incrementStock(item.getVariant().getId(), quantity);
            } else if (item.getProduct() != null) {
                productRepository.incrementStock(item.getProduct().getId(), quantity);
            }
        }
    }

}
