package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.model.entity.WingPoints;
import com.wing.ecommercebackendwing.model.entity.WingPointsTransaction;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import com.wing.ecommercebackendwing.repository.WingPointsRepository;
import com.wing.ecommercebackendwing.repository.WingPointsTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WingPointsService {

    private final WingPointsRepository wingPointsRepository;
    private final WingPointsTransactionRepository wingPointsTransactionRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void addPoints(UUID userId, Integer points, String description, UUID orderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        WingPoints wingPoints = wingPointsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    WingPoints wp = new WingPoints();
                    wp.setUser(user);
                    wp.setPoints(0);
                    wp.setTotalEarned(0);
                    wp.setTotalSpent(0);
                    wp.setUpdatedAt(Instant.now());
                    return wp;
                });

        wingPoints.setPoints(wingPoints.getPoints() + points);
        wingPoints.setTotalEarned(wingPoints.getTotalEarned() + points);
        wingPoints.setUpdatedAt(Instant.now());
        wingPointsRepository.save(wingPoints);

        WingPointsTransaction transaction = new WingPointsTransaction();
        transaction.setUser(user);
        transaction.setPoints(points);
        transaction.setType("EARN");
        transaction.setDescription(description);
        transaction.setCreatedAt(Instant.now());
        if (orderId != null) {
            orderRepository.findById(orderId).ifPresent(transaction::setOrder);
        }
        wingPointsTransactionRepository.save(transaction);
    }

    @Transactional
    public void redeemPoints(UUID userId, Integer points) {
        WingPoints wingPoints = wingPointsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wing points account not found"));

        if (wingPoints.getPoints() < points) {
            throw new RuntimeException("Insufficient points balance");
        }

        wingPoints.setPoints(wingPoints.getPoints() - points);
        wingPoints.setTotalSpent(wingPoints.getTotalSpent() + points);
        wingPoints.setUpdatedAt(Instant.now());
        wingPointsRepository.save(wingPoints);

        WingPointsTransaction transaction = new WingPointsTransaction();
        transaction.setUser(wingPoints.getUser());
        transaction.setPoints(-points);
        transaction.setType("REDEEM");
        transaction.setDescription("Points redeemed for order discount");
        transaction.setCreatedAt(Instant.now());
        wingPointsTransactionRepository.save(transaction);
    }

    public Integer getPointsBalance(UUID userId) {
        return wingPointsRepository.findByUserId(userId)
                .map(WingPoints::getPoints)
                .orElse(0);
    }
}
