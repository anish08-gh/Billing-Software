package com.shreedatta.BillingSoftware.Repository;

import com.shreedatta.BillingSoftware.Entity.Bill;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface BillRepository extends JpaRepository<Bill, UUID> {

    Optional<Bill> findByIdAndShopId(UUID billId, UUID shopId);

    Page<Bill> findByShopIdAndBillStatusOrderByCreatedAtDesc(
            UUID shopId,
            Bill.BillStatus billStatus,
            Pageable pageable
    );

    boolean existsByShopIdAndBillNumber(UUID shopId, String billNumber);

    Page<Bill> findByShopIdOrderByCreatedAtDesc(
            UUID shopId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT b
    FROM Bill b
    WHERE b.id = :billId
      AND b.shop.id = :shopId
""")
    Optional<Bill> findByIdAndShopIdForUpdate(
            @Param("billId") UUID billId,
            @Param("shopId") UUID shopId
    );

    @Query("""
        SELECT b
        FROM Bill b
        WHERE b.shop.id = :shopId
          AND (
              LOWER(b.customerName) LIKE LOWER(CONCAT('%', :query, '%'))
              OR b.customerPhone LIKE CONCAT('%', :query, '%')
          )
        ORDER BY b.createdAt DESC
        """)
    Page<Bill> searchByCustomerNameOrPhone(
            @Param("shopId") UUID shopId,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
    SELECT
        COUNT(b),
        COALESCE(SUM(b.totalAmount), 0)
    FROM Bill b
    WHERE b.shop.id = :shopId
      AND b.billStatus = :billStatus
    """)
    Object[] getDashboardBillStats(
            @Param("shopId") UUID shopId,
            @Param("billStatus") Bill.BillStatus billStatus
    );

    long countByShopIdAndBillStatusAndPaymentStatusNot(
            UUID shopId,
            Bill.BillStatus billStatus,
            Bill.PaymentStatus paymentStatus
    );
}

