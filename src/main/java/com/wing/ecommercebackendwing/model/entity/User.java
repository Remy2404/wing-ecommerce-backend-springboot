package com.wing.ecommercebackendwing.model.entity;

import com.wing.ecommercebackendwing.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    private String avatar;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Email Verification
    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @Column(name = "email_verification_sent_at")
    private Instant emailVerificationSentAt;

    // Account Security
    @Column(name = "account_locked")
    private Boolean accountLocked = false;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_failed_login")
    private Instant lastFailedLogin;

    // Two-Factor Authentication
    @Column(name = "twofa_enabled")
    private Boolean twofaEnabled = false;

    @Column(name = "twofa_secret")
    private String twofaSecret;

    // Password Reset
    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private Instant passwordResetTokenExpiry;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Address> addresses;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Review> reviews;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Merchant merchant;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private WingPoints wingPoints;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL)
    private List<Delivery> deliveries;
}
