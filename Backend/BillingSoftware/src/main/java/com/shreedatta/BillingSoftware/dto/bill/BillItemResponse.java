package com.shreedatta.BillingSoftware.dto.bill;

import java.math.BigDecimal;
import java.util.UUID;

public record BillItemResponse(
        UUID id,
        UUID itemId,
        String itemName,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        Integer lineNumber
) {
}


