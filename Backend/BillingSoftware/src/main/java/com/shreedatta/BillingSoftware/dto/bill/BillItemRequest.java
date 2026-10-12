package com.shreedatta.BillingSoftware.dto.bill;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record BillItemRequest(

        @NotNull(message = "Item ID is required")
        UUID itemId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(
                value = "0.001",
                message = "Quantity must be greater than zero"
        )
        @Digits(
                integer = 9,
                fraction = 3,
                message = "Quantity can have at most 9 integer digits and 3 decimal places"
        )
        BigDecimal quantity
) {
}
