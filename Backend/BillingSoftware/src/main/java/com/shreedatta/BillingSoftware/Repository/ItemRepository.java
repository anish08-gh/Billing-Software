package com.shreedatta.BillingSoftware.Repository;

import com.shreedatta.BillingSoftware.Entity.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    Optional<Item> findByIdAndShopId(UUID id, UUID shopId);

    List<Item> findByShopIdAndActiveTrueOrderByNameAsc(UUID shopId);

    List<Item> findByShopIdAndActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(
            UUID shopId,
            String name,
            Pageable pageable
    );

    boolean existsByShopIdAndSku(UUID shopId, String sku);

    boolean existsByShopIdAndSkuAndIdNot(
            UUID shopId,
            String sku,
            UUID id
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        UPDATE items
        SET price = ROUND(
            price * (1 + CAST(:percentage AS NUMERIC) / 100),
            2
        ),
        updated_at = CURRENT_TIMESTAMP
        WHERE shop_id = :shopId
        """, nativeQuery = true)
    int increasePricesForShop(
            @Param("shopId") UUID shopId,
            @Param("percentage") BigDecimal percentage
    );
}
