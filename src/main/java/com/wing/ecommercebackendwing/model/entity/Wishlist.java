package com.wing.ecommercebackendwing.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wishlists")
@Data
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}