package com.wing.ecommercebackendwing.repository;

import com.wing.ecommercebackendwing.model.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserId(UUID userId);
    Optional<Address> findByUserIdAndIsDefaultTrue(UUID userId);

    @Query("select a from Address a " +
            "where exists (select 1 from Order o where o.deliveryAddress = a) " +
            "  and lower(coalesce(a.label, '')) = lower(:label))")
    List<Address> findOrderLinkedAddressesByLabel(String label);
}
