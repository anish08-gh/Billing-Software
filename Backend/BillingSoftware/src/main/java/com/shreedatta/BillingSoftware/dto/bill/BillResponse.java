package com.shreedatta.BillingSoftware.dto.bill;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BillResponse(
        UUID id,
        String billNumber,
        String customerName,
        String customerPhone,
        List<BillItemResponse> items,
        BigDecimal subtotal,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal pendingAmount,
        String paymentStatus,
        String billStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long version
) {
    public BillResponse {
        items = List.copyOf(items);
    }
}