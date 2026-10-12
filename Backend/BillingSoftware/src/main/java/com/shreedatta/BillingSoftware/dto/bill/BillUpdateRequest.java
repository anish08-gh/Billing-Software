package com.shreedatta.BillingSoftware.dto.bill;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BillUpdateRequest(

        @NotBlank(message = "Customer name is required")
        @Size(max = 150, message = "Customer name cannot exceed 150 characters")
        String customerName,

        @NotBlank(message = "Customer mobile number is required")
        @Pattern(
                regexp = "^[6-9][0-9]{9}$",
                message = "Enter a valid 10-digit Indian mobile number"
        )
        String customerPhone,

        @NotEmpty(message = "A bill must contain at least one item")
        List<@Valid BillItemRequest> items,

        @jakarta.validation.constraints.NotNull(
                message = "Expected bill version is required"
        )
        @jakarta.validation.constraints.PositiveOrZero(
                message = "Expected bill version cannot be negative"
        )
        Long expectedVersion
) {
}
