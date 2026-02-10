package com.wing.ecommercebackendwing.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
@Data
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String sku;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock = 0;

    private String image;

    private String attributes;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}