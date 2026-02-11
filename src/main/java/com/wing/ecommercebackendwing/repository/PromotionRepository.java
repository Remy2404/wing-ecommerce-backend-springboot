package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.Promotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {
    Optional<Promotion> findByCode(String code);
    Optional<Promotion> findByCodeIgnoreCase(String code);
    List<Promotion> findByIsActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE LOWER(p.code) = LOWER(:code)")
    Optional<Promotion> findByCodeIgnoreCaseForUpdate(@Param("code") String code);
}
