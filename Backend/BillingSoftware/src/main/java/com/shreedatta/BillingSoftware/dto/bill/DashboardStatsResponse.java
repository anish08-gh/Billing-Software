package com.shreedatta.BillingSoftware.dto.bill;

import java.math.BigDecimal;

public record DashboardStatsResponse(
        long totalBills,
        BigDecimal totalRevenue,
        BigDecimal totalCollected,
        BigDecimal pendingPayments,
        long outstandingBills
) {
}