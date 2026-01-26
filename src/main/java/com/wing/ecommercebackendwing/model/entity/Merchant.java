package com.wing.ecommercebackendwing.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "merchants")
@Data
public class Merchant {
     @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.AUTO)
    private java.util.UUID id;
     @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
        @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;
        @Column(name = "store_description", length = 500)
    private String storeDescription;
        private String logo;
        private String banner;
        @Column(name = "phone_number", length = 20)
    private String phoneNumber;
        @Column(length = 255)
    private String email;
        @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;
        @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;
        @Column(precision = 3 , scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;
        @Column(precision =5 , scale = 2)
    private BigDecimal commissionRate = BigDecimal.ZERO;
        @Column(name = "created_at", nullable = false)
    private Instant createdAt;
        @Column(name = "updated_at" , nullable = false)
    private Instant updatedAt;
        @OneToMany(mappedBy = "merchant" , cascade = CascadeType.ALL)
    private List<Product> products;
    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL)
    private List<Order> orders;
}
