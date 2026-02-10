package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = CURRENT_TIMESTAMP WHERE rt.user.id = :userId AND rt.revoked = false")
    void revokeAllByUserId(@Param("userId") UUID userId);

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    Optional<RefreshToken> findTopByUserIdAndRevokedFalseOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = CURRENT_TIMESTAMP WHERE rt.token = :token AND rt.revoked = false")
    int revokeIfActive(@Param("token") String token);
}
