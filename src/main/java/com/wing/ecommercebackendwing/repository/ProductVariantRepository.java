package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findByProductId(UUID productId);

    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.stock = pv.stock - :quantity WHERE pv.id = :variantId AND pv.stock >= :quantity")
    int decrementStockIfAvailable(@Param("variantId") UUID variantId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.stock = pv.stock + :quantity WHERE pv.id = :variantId")
    int incrementStock(@Param("variantId") UUID variantId, @Param("quantity") int quantity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id = :variantId")
    java.util.Optional<ProductVariant> findByIdForUpdate(@Param("variantId") UUID variantId);
}
