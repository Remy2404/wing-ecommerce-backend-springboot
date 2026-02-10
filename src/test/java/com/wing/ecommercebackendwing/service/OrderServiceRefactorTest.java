package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.request.order.CreateOrderRequest;
import com.wing.ecommercebackendwing.dto.request.order.OrderItemRequest;
import com.wing.ecommercebackendwing.dto.request.order.ShippingAddressRequest;
import com.wing.ecommercebackendwing.dto.response.order.OrderResponse;
import com.wing.ecommercebackendwing.exception.custom.BadRequestException;
import com.wing.ecommercebackendwing.exception.custom.ForbiddenException;
import com.wing.ecommercebackendwing.exception.custom.ResourceNotFoundException;
import com.wing.ecommercebackendwing.model.entity.*;
import com.wing.ecommercebackendwing.model.enums.OrderStatus;
import com.wing.ecommercebackendwing.model.enums.UserRole;
import com.wing.ecommercebackendwing.repository.*;
import com.wing.ecommercebackendwing.util.OrderNumberGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceRefactorTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderNumberGenerator orderNumberGenerator;
    @Mock private TaxService taxService;
    @Mock private DeliveryFeeService deliveryFeeService;
    @Mock private DiscountService discountService;
    @Mock private OrderIdempotencyRecordRepository orderIdempotencyRecordRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private PhoneNumberService phoneNumberService;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Merchant merchant;
    private Product product;
    private UUID userId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        
        merchant = new Merchant();
        merchant.setId(UUID.randomUUID());
        
        product = new Product();
        product.setId(productId);
        product.setName("Test Product");
        product.setPrice(BigDecimal.TEN);
        product.setStockQuantity(100);
        product.setMerchant(merchant);
        lenient().when(phoneNumberService.normalizeToE164(anyString(), any())).thenReturn("+855962026409");
    }

    @Test
    void createOrder_BuyNow_ShouldDecrementStockAndKeepCart() {
        // Arrange
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(productId);
        itemReq.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemReq));
        request.setShippingAddress(ShippingAddressRequest.builder()
                .fullName("Buyer")
                .phone("09620264091")
                .country("KH")
                .street("Street").city("City").state("State").zipCode("12345").build());
        request.setPaymentMethod("KHQR");

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setItems(new ArrayList<>());
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.decrementStockIfAvailable(productId, 2)).thenReturn(1);
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArguments()[0]);
        when(orderNumberGenerator.generateOrderNumber()).thenReturn("ORD-123");
        when(taxService.calculateTax(any())).thenReturn(BigDecimal.ZERO);
        when(deliveryFeeService.calculateFee(any())).thenReturn(BigDecimal.ZERO);
        when(discountService.calculateDiscount(any(), any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        OrderResponse result = orderService.createOrder(userId, request);

        // Assert
        verify(productRepository).decrementStockIfAvailable(productId, 2);
        verify(productRepository, never()).save(any(Product.class));
        verify(cartRepository, never()).save(cart); // Cart should NOT be saved/cleared
        assertNotNull(result);
        assertEquals("ORD-123", result.getOrderNumber());
    }

    @Test
    void createOrder_FromCart_ShouldDecrementStockAndClearCart() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(null); // Checkout from cart
        request.setShippingAddress(ShippingAddressRequest.builder()
                .fullName("Buyer")
                .phone("09620264091")
                .country("KH")
                .street("Street").city("City").state("State").zipCode("12345").build());
        request.setPaymentMethod("KHQR");

        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(3);
        cartItem.setPrice(BigDecimal.TEN);
        
        Cart cart = new Cart();
        cart.setUser(user);
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(cartItem);
        cart.setItems(cartItems);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.decrementStockIfAvailable(productId, 3)).thenReturn(1);
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArguments()[0]);
        when(orderNumberGenerator.generateOrderNumber()).thenReturn("ORD-123");
        when(taxService.calculateTax(any())).thenReturn(BigDecimal.ZERO);
        when(deliveryFeeService.calculateFee(any())).thenReturn(BigDecimal.ZERO);
        when(discountService.calculateDiscount(any(), any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        OrderResponse result = orderService.createOrder(userId, request);

        // Assert
        verify(productRepository).decrementStockIfAvailable(productId, 3);
        verify(productRepository, never()).save(any(Product.class));
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart); // Cart SHOULD be cleared
    }

    @Test
    void createOrder_MixedMerchants_ShouldThrowException() {
        // Arrange
        Merchant otherMerchant = new Merchant();
        otherMerchant.setId(UUID.randomUUID());
        
        Product otherProduct = new Product();
        otherProduct.setId(UUID.randomUUID());
        otherProduct.setMerchant(otherMerchant);
        otherProduct.setStockQuantity(10);

        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId(productId);
        item1.setQuantity(1);

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId(otherProduct.getId());
        item2.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(item1, item2));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.findById(otherProduct.getId())).thenReturn(Optional.of(otherProduct));
        when(productRepository.decrementStockIfAvailable(productId, 1)).thenReturn(1);
        when(productRepository.decrementStockIfAvailable(otherProduct.getId(), 1)).thenReturn(1);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.createOrder(userId, request));
    }

    @Test
    void createOrder_VariantProductMismatch_ShouldRejectWithoutSideEffects() {
        UUID variantId = UUID.randomUUID();

        Product otherProduct = new Product();
        otherProduct.setId(UUID.randomUUID());

        ProductVariant mismatchedVariant = new ProductVariant();
        mismatchedVariant.setId(variantId);
        mismatchedVariant.setProduct(otherProduct);
        mismatchedVariant.setStock(5);
        mismatchedVariant.setPrice(new BigDecimal("2.50"));

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(productId);
        itemReq.setVariantId(variantId);
        itemReq.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemReq));
        request.setShippingAddress(ShippingAddressRequest.builder()
                .fullName("Buyer")
                .phone("09620264091")
                .country("KH")
                .street("Street").city("City").state("State").zipCode("12345").build());
        request.setPaymentMethod("KHQR");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(mismatchedVariant));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> orderService.createOrder(userId, request));

        assertEquals(100, product.getStockQuantity());
        verify(productVariantRepository, never()).save(any(ProductVariant.class));
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartRepository, never()).save(any(Cart.class));
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void createOrder_BuyNow_ShouldRejectWhenAtomicDecrementFails_WithoutPartialWrites() {
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(productId);
        itemReq.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemReq));
        request.setShippingAddress(ShippingAddressRequest.builder()
                .fullName("Buyer")
                .phone("09620264091")
                .country("KH")
                .street("Street").city("City").state("State").zipCode("12345").build());
        request.setPaymentMethod("KHQR");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(productRepository.decrementStockIfAvailable(productId, 2)).thenReturn(0);

        assertThrows(BadRequestException.class, () -> orderService.createOrder(userId, request));

        verify(productRepository).decrementStockIfAvailable(productId, 2);
        verify(orderRepository, never()).save(any(Order.class));
        verify(addressRepository, never()).save(any(Address.class));
        verify(cartRepository, never()).save(any(Cart.class));
        verify(productVariantRepository, never()).decrementStockIfAvailable(any(UUID.class), anyInt());
    }

    @Test
    void createOrder_IdempotencySameKeyAndPayload_ShouldReturnExistingOrderWithoutMutation() throws Exception {
        UUID existingOrderId = UUID.randomUUID();
        String idemKey = "idem-123";

        Order existingOrder = new Order();
        existingOrder.setId(existingOrderId);
        existingOrder.setOrderNumber("ORD-EXISTING");
        existingOrder.setStatus(OrderStatus.PENDING);
        existingOrder.setTotal(BigDecimal.TEN);
        existingOrder.setSubtotal(BigDecimal.TEN);
        existingOrder.setDeliveryFee(BigDecimal.ZERO);
        existingOrder.setDiscount(BigDecimal.ZERO);
        existingOrder.setTax(BigDecimal.ZERO);
        existingOrder.setUser(user);
        existingOrder.setCreatedAt(java.time.Instant.now());
        existingOrder.setUpdatedAt(java.time.Instant.now());
        existingOrder.setItems(new ArrayList<>());

        OrderIdempotencyRecord record = new OrderIdempotencyRecord();
        record.setId(UUID.randomUUID());
        record.setUser(user);
        record.setIdempotencyKey(idemKey);
        record.setRequestHash("hash-1");
        record.setOrder(existingOrder);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(OrderItemRequest.builder().productId(productId).quantity(1).build()));
        request.setPaymentMethod("KHQR");
        request.setShippingAddress(ShippingAddressRequest.builder()
                .fullName("Buyer")
                .phone("09620264091")
                .country("KH")
                .street("Street").city("City").state("State").zipCode("12345").build());

        when(objectMapper.writeValueAsString(request)).thenReturn("{\"req\":1}");
        when(orderIdempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, idemKey))
                .thenReturn(Optional.of(record));

        String expectedHash = java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest("{\"req\":1}".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
        record.setRequestHash(expectedHash);

        OrderResponse response = orderService.createOrder(userId, request, idemKey);

        assertEquals("ORD-EXISTING", response.getOrderNumber());
        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).decrementStockIfAvailable(any(UUID.class), anyInt());
        verify(productVariantRepository, never()).decrementStockIfAvailable(any(UUID.class), anyInt());
    }

    @Test
    void createOrder_IdempotencySameKeyDifferentPayload_ShouldRejectWithoutWrites() throws Exception {
        String idemKey = "idem-123";
        OrderIdempotencyRecord record = new OrderIdempotencyRecord();
        record.setId(UUID.randomUUID());
        record.setUser(user);
        record.setIdempotencyKey(idemKey);
        record.setRequestHash("different-hash");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(OrderItemRequest.builder().productId(productId).quantity(1).build()));
        request.setPaymentMethod("KHQR");
        request.setShippingAddress(ShippingAddressRequest.builder()
                .fullName("Buyer")
                .phone("09620264091")
                .country("KH")
                .street("Street").city("City").state("State").zipCode("12345").build());

        when(objectMapper.writeValueAsString(request)).thenReturn("{\"req\":1}");
        when(orderIdempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, idemKey))
                .thenReturn(Optional.of(record));

        assertThrows(BadRequestException.class, () -> orderService.createOrder(userId, request, idemKey));

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderIdempotencyRecordRepository, never()).save(any(OrderIdempotencyRecord.class));
        verify(productRepository, never()).decrementStockIfAvailable(any(UUID.class), anyInt());
        verify(productVariantRepository, never()).decrementStockIfAvailable(any(UUID.class), anyInt());
    }

    @Test
    void updateOrderStatus_ShouldRejectIllegalTransition() {
        UUID orderId = UUID.randomUUID();
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setRole(UserRole.ADMIN);

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.DELIVERED);
        order.setUser(user);
        order.setItems(new ArrayList<>());
        order.setSubtotal(BigDecimal.ZERO);
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setDiscount(BigDecimal.ZERO);
        order.setTax(BigDecimal.ZERO);
        order.setTotal(BigDecimal.ZERO);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThrows(BadRequestException.class,
                () -> orderService.updateOrderStatus(orderId, OrderStatus.PENDING, admin.getId()));

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_ShouldAllowLegalTransition() {
        UUID orderId = UUID.randomUUID();
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setRole(UserRole.ADMIN);

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING);
        order.setUser(user);
        order.setItems(new ArrayList<>());
        order.setSubtotal(BigDecimal.ZERO);
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setDiscount(BigDecimal.ZERO);
        order.setTax(BigDecimal.ZERO);
        order.setTotal(BigDecimal.ZERO);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED, admin.getId());

        assertEquals("CONFIRMED", response.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_CustomerCancelOnlyPendingOwnOrder() {
        UUID orderId = UUID.randomUUID();
        user.setRole(UserRole.CUSTOMER);

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PAID);
        order.setUser(user);
        order.setItems(new ArrayList<>());
        order.setSubtotal(BigDecimal.ZERO);
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setDiscount(BigDecimal.ZERO);
        order.setTax(BigDecimal.ZERO);
        order.setTotal(BigDecimal.ZERO);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class,
                () -> orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED, userId));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_Cancel_ShouldRestoreStockForProductItem() {
        UUID orderId = UUID.randomUUID();
        user.setRole(UserRole.CUSTOMER);

        Product orderProduct = new Product();
        UUID orderProductId = UUID.randomUUID();
        orderProduct.setId(orderProductId);

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(orderProduct);
        orderItem.setQuantity(3);
        orderItem.setUnitPrice(BigDecimal.ONE);

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING);
        order.setUser(user);
        order.setItems(new ArrayList<>(List.of(orderItem)));
        orderItem.setOrder(order);
        order.setSubtotal(BigDecimal.ZERO);
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setDiscount(BigDecimal.ZERO);
        order.setTax(BigDecimal.ZERO);
        order.setTotal(BigDecimal.ZERO);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);
        when(productRepository.incrementStock(orderProductId, 3)).thenReturn(1);

        OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED, userId);

        assertEquals("CANCELLED", response.getStatus());
        verify(productRepository).incrementStock(orderProductId, 3);
        verify(productVariantRepository, never()).incrementStock(any(UUID.class), anyInt());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_CancelSameStatus_ShouldNotRestoreStockTwice() {
        UUID orderId = UUID.randomUUID();
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setRole(UserRole.ADMIN);

        Product orderProduct = new Product();
        UUID orderProductId = UUID.randomUUID();
        orderProduct.setId(orderProductId);

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(orderProduct);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(BigDecimal.ONE);

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.CANCELLED);
        order.setUser(user);
        order.setItems(new ArrayList<>(List.of(orderItem)));
        orderItem.setOrder(order);
        order.setSubtotal(BigDecimal.ZERO);
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setDiscount(BigDecimal.ZERO);
        order.setTax(BigDecimal.ZERO);
        order.setTotal(BigDecimal.ZERO);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED, admin.getId());

        assertEquals("CANCELLED", response.getStatus());
        verify(productRepository, never()).incrementStock(orderProductId, 2);
        verify(productVariantRepository, never()).incrementStock(any(UUID.class), anyInt());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_CustomerCannotCancelOtherUsersOrder_ShouldThrowForbidden() {
        UUID orderId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        User customer = new User();
        customer.setId(userId);
        customer.setRole(UserRole.CUSTOMER);

        User orderOwner = new User();
        orderOwner.setId(otherUserId);
        orderOwner.setRole(UserRole.CUSTOMER);

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING);
        order.setUser(orderOwner);
        order.setItems(new ArrayList<>());
        order.setSubtotal(BigDecimal.ZERO);
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setDiscount(BigDecimal.ZERO);
        order.setTax(BigDecimal.ZERO);
        order.setTotal(BigDecimal.ZERO);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.of(customer));

        assertThrows(ForbiddenException.class,
                () -> orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED, userId));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_WhenOrderNotFound_ShouldThrowResourceNotFound() {
        UUID orderId = UUID.randomUUID();
        UUID requester = UUID.randomUUID();

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED, requester));
        verify(orderRepository, never()).save(any(Order.class));
    }
}
