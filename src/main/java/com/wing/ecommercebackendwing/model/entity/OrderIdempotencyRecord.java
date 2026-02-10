package com.wing.ecommercebackendwing.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "order_idempotency_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_order_idempotency_user_key", columnNames = {"user_id", "idempotency_key"})
        }
)
@Data
public class OrderIdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
