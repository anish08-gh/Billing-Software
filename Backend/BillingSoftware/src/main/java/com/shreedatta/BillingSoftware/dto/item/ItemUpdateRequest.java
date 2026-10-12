package com.shreedatta.BillingSoftware.dto.item;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemUpdateRequest(

        @NotBlank(message = "Item name is required")
        @Size(max = 200, message = "Item name must not exceed 200 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @Size(max = 100, message = "SKU must not exceed 100 characters")
        String sku,

        @NotBlank(message = "Unit is required")
        @Size(max = 30, message = "Unit must not exceed 30 characters")
        String unit,

        @DecimalMin(
                value = "0.00",
                inclusive = true,
                message = "Price cannot be negative"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "Price must have at most 10 integer digits and 2 decimal places"
        )
        BigDecimal price
) {
}