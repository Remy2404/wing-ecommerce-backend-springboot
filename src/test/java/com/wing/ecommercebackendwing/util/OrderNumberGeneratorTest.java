package com.wing.ecommercebackendwing.util;

import com.wing.ecommercebackendwing.model.entity.Order;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderNumberGeneratorTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderNumberGenerator orderNumberGenerator;

    private String datePrefix;

    @BeforeEach
    void setUp() {
        datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    @Test
    void generateOrderNumber_WhenNoPreviousOrders_ShouldStartAt001() {
        // Arrange
        when(orderRepository.findFirstByOrderNumberStartingWithOrderByOrderNumberDesc(startsWith("ORD-" + datePrefix)))
                .thenReturn(Optional.empty());

        // Act
        String orderNumber = orderNumberGenerator.generateOrderNumber();

        // Assert
        assertEquals("ORD-" + datePrefix + "-001", orderNumber);
    }

    @Test
    void generateOrderNumber_WhenPreviousOrdersExist_ShouldIncrementSequence() {
        // Arrange
        Order lastOrder = new Order();
        lastOrder.setOrderNumber("ORD-" + datePrefix + "-005");
        
        when(orderRepository.findFirstByOrderNumberStartingWithOrderByOrderNumberDesc(startsWith("ORD-" + datePrefix)))
                .thenReturn(Optional.of(lastOrder));

        // Act
        String orderNumber = orderNumberGenerator.generateOrderNumber();

        // Assert
        assertEquals("ORD-" + datePrefix + "-006", orderNumber);
    }

    @Test
    void generateOrderNumber_WhenLastOrderHasInvalidFormat_ShouldDefaultTo001() {
        // Arrange
        Order lastOrder = new Order();
        lastOrder.setOrderNumber("INVALID-FORMAT");
        
        when(orderRepository.findFirstByOrderNumberStartingWithOrderByOrderNumberDesc(startsWith("ORD-" + datePrefix)))
                .thenReturn(Optional.of(lastOrder));

        // Act
        String orderNumber = orderNumberGenerator.generateOrderNumber();

        // Assert
        assertEquals("ORD-" + datePrefix + "-001", orderNumber);
    }
}
