package com.shreedatta.BillingSoftware.dto.item;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        String name,
        String description,
        String sku,
        String unit,
        BigDecimal price,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}