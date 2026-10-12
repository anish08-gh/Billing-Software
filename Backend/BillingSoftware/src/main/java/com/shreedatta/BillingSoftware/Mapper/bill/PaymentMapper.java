package com.shreedatta.BillingSoftware.Mapper.bill;

import com.shreedatta.BillingSoftware.dto.bill.PaymentResponse;
import com.shreedatta.BillingSoftware.Entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getPaymentMethod().name(),
                payment.getPaymentReference(),
                payment.getNotes(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}

