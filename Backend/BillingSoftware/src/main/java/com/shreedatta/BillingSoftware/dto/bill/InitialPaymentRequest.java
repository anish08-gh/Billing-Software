package com.shreedatta.BillingSoftware.dto.bill;

import com.shreedatta.BillingSoftware.Entity.Payment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InitialPaymentRequest(

        @NotNull(message = "Payment amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Payment amount must be greater than zero"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "Payment amount can have at most 10 integer digits and 2 decimal places"
        )
        BigDecimal amount,

        @NotNull(message = "Payment method is required")
        Payment.PaymentMethod paymentMethod,

        @Size(max = 100, message = "Payment reference cannot exceed 100 characters")
        String paymentReference,

        @Size(max = 2000, message = "Payment notes cannot exceed 2000 characters")
        String notes
) {
}
