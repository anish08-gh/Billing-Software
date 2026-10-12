package com.shreedatta.BillingSoftware.dto.bill;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        BigDecimal amount,
        String paymentMethod,
        String paymentReference,
        String notes,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt
) {
}

