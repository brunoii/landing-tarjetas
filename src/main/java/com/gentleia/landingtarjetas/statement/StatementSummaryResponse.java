package com.gentleia.landingtarjetas.statement;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.Provider;
import com.gentleia.landingtarjetas.shared.StatementStatus;

public record StatementSummaryResponse(
        Long id,
        Provider provider,
        CardBrand cardBrand,
        String cardAlias,
        LocalDate paymentMonth,
        BigDecimal totalPesos,
        BigDecimal totalUsd,
        BigDecimal minimumPaymentPesos,
        StatementStatus status,
        int transactionCount
) {
    public static StatementSummaryResponse from(CardStatement statement) {
        return new StatementSummaryResponse(
                statement.getId(),
                statement.getProvider(),
                statement.getCardBrand(),
                statement.getCardAlias(),
                statement.getPaymentMonth(),
                statement.getTotalPesos(),
                statement.getTotalUsd(),
                statement.getMinimumPaymentPesos(),
                statement.getStatus(),
                statement.getTransactions().size()
        );
    }
}
