package com.shreedatta.BillingSoftware.Mapper.bill;

import com.shreedatta.BillingSoftware.dto.bill.BillItemResponse;
import com.shreedatta.BillingSoftware.dto.bill.BillResponse;
import com.shreedatta.BillingSoftware.Entity.Bill;
import com.shreedatta.BillingSoftware.Entity.BillItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class BillMapper {

    public BillItemResponse toBillItemResponse(BillItem billItem) {
        return new BillItemResponse(
                billItem.getId(),
                billItem.getItem() == null
                        ? null
                        : billItem.getItem().getId(),
                billItem.getItemName(),
                billItem.getUnit(),
                billItem.getQuantity(),
                billItem.getUnitPrice(),
                billItem.getTotalAmount(),
                billItem.getLineNumber()
        );
    }

    public BillResponse toBillResponse(
            Bill bill,
            List<BillItem> billItems,
            BigDecimal amountPaid
    ) {
        BigDecimal normalizedAmountPaid = amountPaid == null
                ? BigDecimal.ZERO
                : amountPaid;

        BigDecimal pendingAmount = bill.getTotalAmount()
                .subtract(normalizedAmountPaid);

        List<BillItemResponse> itemResponses = billItems.stream()
                .map(this::toBillItemResponse)
                .toList();

        return new BillResponse(
                bill.getId(),
                bill.getBillNumber(),
                bill.getCustomerName(),
                bill.getCustomerPhone(),
                itemResponses,
                bill.getSubtotal(),
                bill.getTotalAmount(),
                normalizedAmountPaid,
                pendingAmount,
                bill.getPaymentStatus().name(),
                bill.getBillStatus().name(),
                bill.getCreatedAt(),
                bill.getUpdatedAt(),
                bill.getVersion()
        );
    }
}


