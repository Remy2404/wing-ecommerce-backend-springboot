package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.model.entity.Order;
import com.wing.ecommercebackendwing.model.entity.User;
import com.wing.ecommercebackendwing.model.entity.WingPoints;
import com.wing.ecommercebackendwing.model.entity.WingPointsTransaction;
import com.wing.ecommercebackendwing.repository.OrderRepository;
import com.wing.ecommercebackendwing.repository.UserRepository;
import com.wing.ecommercebackendwing.repository.WingPointsRepository;
import com.wing.ecommercebackendwing.repository.WingPointsTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WingPointsService {

    private static final int MAX_POINTS_PER_ORDER = 10_000;
    private static final String EARN_TYPE = "EARN";
    private static final String REDEEM_TYPE = "REDEEM";
    private static final String REVERSAL_TYPE = "REVERSAL";

    private final WingPointsRepository wingPointsRepository;
    private final WingPointsTransactionRepository wingPointsTransactionRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void addPoints(UUID userId, Integer points, String description, UUID orderId) {
        if (orderId == null) {
            throw new RuntimeException("Order reference is required to award points");
        }

        if (wingPointsTransactionRepository.existsByUserIdAndOrderIdAndType(userId, orderId, EARN_TYPE)) {
            return;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Order does not belong to the user");
        }

        int expectedPoints = calculateEarnPoints(order.getTotalAmount());
        if (expectedPoints <= 0) {
            return;
        }

        if (points != null && points > 0 && points != expectedPoints) {
            log.warn("Ignoring externally supplied points. userId={}, orderId={}, supplied={}, expected={}",
                    userId, orderId, points, expectedPoints);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        WingPoints wingPoints = getOrCreateWingPoints(userId, user);
        wingPoints.setPoints(wingPoints.getPoints() + expectedPoints);
        wingPoints.setTotalEarned(wingPoints.getTotalEarned() + expectedPoints);
        wingPoints.setUpdatedAt(Instant.now());
        wingPointsRepository.save(wingPoints);

        WingPointsTransaction transaction = new WingPointsTransaction();
        transaction.setUser(user);
        transaction.setOrder(order);
        transaction.setPoints(expectedPoints);
        transaction.setType(EARN_TYPE);
        transaction.setDescription(description != null ? description : "Points earned from paid order");
        transaction.setCreatedAt(Instant.now());
        wingPointsTransactionRepository.save(transaction);
    }

    @Transactional
    public void redeemPoints(UUID userId, Integer points) {
        if (points == null || points <= 0) {
            throw new RuntimeException("Points must be greater than zero");
        }

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
        transaction.setType(REDEEM_TYPE);
        transaction.setDescription("Points redeemed for order discount");
        transaction.setCreatedAt(Instant.now());
        wingPointsTransactionRepository.save(transaction);
    }

    @Transactional
    public void revokeEarnedPointsForOrder(UUID userId, UUID orderId) {
        if (orderId == null || userId == null) {
            return;
        }

        Optional<WingPointsTransaction> earnedTransactionOpt =
                wingPointsTransactionRepository.findFirstByUserIdAndOrderIdAndType(userId, orderId, EARN_TYPE);
        if (earnedTransactionOpt.isEmpty()) {
            return;
        }

        if (wingPointsTransactionRepository.existsByUserIdAndOrderIdAndType(userId, orderId, REVERSAL_TYPE)) {
            return;
        }

        WingPointsTransaction earnedTransaction = earnedTransactionOpt.get();
        int earnedPoints = Math.max(0, earnedTransaction.getPoints() != null ? earnedTransaction.getPoints() : 0);
        if (earnedPoints == 0) {
            return;
        }

        User user = earnedTransaction.getUser() != null
                ? earnedTransaction.getUser()
                : userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        WingPoints wingPoints = getOrCreateWingPoints(userId, user);

        int currentBalance = wingPoints.getPoints() != null ? wingPoints.getPoints() : 0;
        int currentTotalEarned = wingPoints.getTotalEarned() != null ? wingPoints.getTotalEarned() : 0;
        wingPoints.setPoints(Math.max(0, currentBalance - earnedPoints));
        wingPoints.setTotalEarned(Math.max(0, currentTotalEarned - earnedPoints));
        wingPoints.setUpdatedAt(Instant.now());
        wingPointsRepository.save(wingPoints);

        WingPointsTransaction reversalTransaction = new WingPointsTransaction();
        reversalTransaction.setUser(user);
        reversalTransaction.setOrder(earnedTransaction.getOrder());
        reversalTransaction.setPoints(-earnedPoints);
        reversalTransaction.setType(REVERSAL_TYPE);
        reversalTransaction.setDescription("Points reversed due to order cancellation");
        reversalTransaction.setCreatedAt(Instant.now());
        wingPointsTransactionRepository.save(reversalTransaction);
    }

    public Integer getPointsBalance(UUID userId) {
        return wingPointsRepository.findByUserId(userId)
                .map(WingPoints::getPoints)
                .orElse(0);
    }

    private WingPoints getOrCreateWingPoints(UUID userId, User user) {
        return wingPointsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    WingPoints wp = new WingPoints();
                    wp.setUser(user);
                    wp.setPoints(0);
                    wp.setTotalEarned(0);
                    wp.setTotalSpent(0);
                    wp.setUpdatedAt(Instant.now());
                    return wp;
                });
    }

    private int calculateEarnPoints(BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        int points = totalAmount.setScale(0, RoundingMode.DOWN).intValue();
        return Math.min(Math.max(points, 0), MAX_POINTS_PER_ORDER);
    }
}
