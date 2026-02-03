package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.dto.request.order.CreateOrderRequest;
import com.wing.ecommercebackendwing.dto.request.order.OrderItemRequest;
import com.wing.ecommercebackendwing.dto.request.order.ShippingAddressRequest;
import com.wing.ecommercebackendwing.dto.response.order.OrderResponse;
import com.wing.ecommercebackendwing.model.entity.*;
import com.wing.ecommercebackendwing.model.enums.OrderStatus;
import com.wing.ecommercebackendwing.repository.*;
import com.wing.ecommercebackendwing.util.OrderNumberGenerator;
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
                .street("Street").city("City").state("State").zipCode("12345").build());
        request.setPaymentMethod("KHQR");

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setItems(new ArrayList<>());
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArguments()[0]);
        when(orderNumberGenerator.generateOrderNumber()).thenReturn("ORD-123");
        when(taxService.calculateTax(any())).thenReturn(BigDecimal.ZERO);
        when(deliveryFeeService.calculateFee(any())).thenReturn(BigDecimal.ZERO);
        when(discountService.calculateDiscount(any(), any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        OrderResponse result = orderService.createOrder(userId, request);

        // Assert
        assertEquals(98, product.getStockQuantity());
        verify(productRepository).save(product);
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
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArguments()[0]);
        when(orderNumberGenerator.generateOrderNumber()).thenReturn("ORD-123");
        when(taxService.calculateTax(any())).thenReturn(BigDecimal.ZERO);
        when(deliveryFeeService.calculateFee(any())).thenReturn(BigDecimal.ZERO);
        when(discountService.calculateDiscount(any(), any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        OrderResponse result = orderService.createOrder(userId, request);

        // Assert
        assertEquals(97, product.getStockQuantity());
        verify(productRepository).save(product);
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

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.createOrder(userId, request));
    }
}
