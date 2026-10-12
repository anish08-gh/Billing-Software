package com.shreedatta.BillingSoftware.Repository;

import com.shreedatta.BillingSoftware.Entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BillItemRepository extends JpaRepository<BillItem, UUID> {

    List<BillItem> findByBillIdOrderByLineNumberAsc(UUID billId);

    long countByBillId(UUID billId);
}