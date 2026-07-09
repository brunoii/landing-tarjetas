package com.gentleia.landingtarjetas.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardSummaryResponse(
        LocalDate paymentMonth,
        BigDecimal totalPesos,
        BigDecimal totalUsd,
        BigDecimal incomeTotalPesos,
        BigDecimal salaryIncomeTotalPesos,
        BigDecimal variableIncomeTotalPesos,
        BigDecimal projectedIncomeTotalPesos,
        BigDecimal expenseTotalPesos,
        BigDecimal monthlyBalancePesos,
        boolean estimated,
        long statementCount,
        long transactionCount,
        long incomeCount
) {
}
