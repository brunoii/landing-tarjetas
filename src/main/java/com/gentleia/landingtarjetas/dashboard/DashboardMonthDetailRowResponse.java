package com.gentleia.landingtarjetas.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.TransactionType;

public record DashboardMonthDetailRowResponse(
        String kind,
        Long sourceStatementId,
        LocalDate sourceStatementMonth,
        Long sourceTransactionId,
        LocalDate month,
        String description,
        CardBrand cardBrand,
        String cardAlias,
        TransactionType type,
        Long categoryId,
        String categoryName,
        Integer installmentNumber,
        Integer totalInstallments,
        BigDecimal amountPesos,
        BigDecimal amountUsd,
        LocalDate estimatedFinishMonth
) {
}
