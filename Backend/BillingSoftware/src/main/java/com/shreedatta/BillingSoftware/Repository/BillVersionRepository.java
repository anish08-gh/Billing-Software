package com.shreedatta.BillingSoftware.Repository;

import com.shreedatta.BillingSoftware.Entity.BillVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BillVersionRepository
        extends JpaRepository<BillVersion, UUID> {

    List<BillVersion> findByBillIdOrderByVersionNumberDesc(UUID billId);

    boolean existsByBillIdAndVersionNumber(
            UUID billId,
            long versionNumber
    );
}

