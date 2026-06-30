package com.gentleia.landingtarjetas.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.Provider;
import com.gentleia.landingtarjetas.shared.StatementStatus;
import com.gentleia.landingtarjetas.transaction.TransactionResponse;

public record StatementDetailResponse(
        Long id,
        Provider provider,
        CardBrand cardBrand,
        String cardAlias,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate closingDate,
        LocalDate dueDate,
        LocalDate paymentMonth,
        BigDecimal totalPesos,
        BigDecimal totalUsd,
        BigDecimal minimumPaymentPesos,
        StatementStatus status,
        UploadedFileResponse uploadedFile,
        List<TransactionResponse> transactions
) {
    public static StatementDetailResponse from(CardStatement statement) {
        return new StatementDetailResponse(
                statement.getId(),
                statement.getProvider(),
                statement.getCardBrand(),
                statement.getCardAlias(),
                statement.getPeriodStart(),
                statement.getPeriodEnd(),
                statement.getClosingDate(),
                statement.getDueDate(),
                statement.getPaymentMonth(),
                statement.getTotalPesos(),
                statement.getTotalUsd(),
                statement.getMinimumPaymentPesos(),
                statement.getStatus(),
                UploadedFileResponse.from(statement.getUploadedFile()),
                statement.getTransactions().stream().map(TransactionResponse::from).toList()
        );
    }
}
