package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.repository.WingPointsRepository;
import com.wing.ecommercebackendwing.repository.WingPointsTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WingPointsService {

    private final WingPointsRepository wingPointsRepository;
    private final WingPointsTransactionRepository wingPointsTransactionRepository;

    @Transactional
    public void earnPoints(UUID userId, Object earnRequest) {
        // TODO: Add points to user account
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public void redeemPoints(UUID userId, Object redeemRequest) {
        // TODO: Redeem points for discount
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Object getPointsBalance(UUID userId) {
        // TODO: Get user's points balance
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
