package com.wing.ecommercebackendwing.model.entity;

import com.wing.ecommercebackendwing.model.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
@Data
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User driver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "pickup_time")
    private Instant pickupTime;

    @Column(name = "delivered_time")
    private Instant deliveredTime;

    @Column(name = "current_latitude", precision = 10, scale = 8)
    private BigDecimal currentLatitude;

    @Column(name = "current_longitude", precision = 11, scale = 8)
    private BigDecimal currentLongitude;

    @Column(name = "driver_notes")
    private String driverNotes;

    @Column(name = "photo_proof")
    private String photoProof;

    private String signature;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}