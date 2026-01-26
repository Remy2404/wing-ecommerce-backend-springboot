package com.wing.ecommercebackendwing.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wing_points")
@Data
public class WingPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer points = 0;

    @Column(name = "total_earned", nullable = false)
    private Integer totalEarned = 0;

    @Column(name = "total_spent", nullable = false)
    private Integer totalSpent = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}