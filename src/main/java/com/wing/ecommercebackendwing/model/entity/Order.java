package com.wing.ecommercebackendwing.model.entity;

import com.wing.ecommercebackendwing.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "order_number", nullable = false, length = 50, unique = true)
    private String orderNumber;

    @Column(name = "order_date", nullable = false)
    private Instant orderDate;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_address_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Address deliveryAddress;

    private String deliveryInstructions;

    @Column(name = "estimated_delivery_time")
    private Instant estimatedDeliveryTime;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    private String cancelReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Payment payment;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Delivery delivery;
}