package com.shreedatta.BillingSoftware.Repository;

import com.shreedatta.BillingSoftware.Entity.Bill;
import com.shreedatta.BillingSoftware.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByBillIdOrderByPaidAtDesc(UUID billId);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.bill.id = :billId
        """)
    BigDecimal getTotalPaidByBillId(@Param("billId") UUID billId);

    @Query("""
    SELECT COALESCE(SUM(p.amount), 0)
    FROM Payment p
    WHERE p.bill.shop.id = :shopId
      AND p.bill.billStatus = :billStatus
    """)
    BigDecimal getTotalCollectedByShopId(
            @Param("shopId") UUID shopId,
            @Param("billStatus") Bill.BillStatus billStatus
    );

    long countByBillId(UUID billId);
}