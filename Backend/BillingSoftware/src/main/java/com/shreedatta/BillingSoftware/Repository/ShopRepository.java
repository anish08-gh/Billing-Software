package com.shreedatta.BillingSoftware.Repository;

import com.shreedatta.BillingSoftware.Entity.Shop;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Shop s WHERE s.id = :shopId")
    Optional<Shop> findByIdForUpdate(@Param("shopId") UUID shopId);
}