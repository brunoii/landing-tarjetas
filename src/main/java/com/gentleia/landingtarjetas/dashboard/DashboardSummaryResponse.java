package com.gentleia.landingtarjetas.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardSummaryResponse(
        LocalDate paymentMonth,
        BigDecimal totalPesos,
        BigDecimal totalUsd,
        long statementCount,
        long transactionCount
) {
}
