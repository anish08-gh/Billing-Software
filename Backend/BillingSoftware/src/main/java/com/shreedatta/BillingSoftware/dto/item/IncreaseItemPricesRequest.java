package com.shreedatta.BillingSoftware.dto.item;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record IncreaseItemPricesRequest(

        @NotNull(message = "Percentage is required")
        @DecimalMin(
                value = "0",
                inclusive = false,
                message = "Percentage must be greater than 0"
        )
        @DecimalMax(
                value = "1000",
                message = "Percentage cannot exceed 1000"
        )
        BigDecimal percentage

) {
}