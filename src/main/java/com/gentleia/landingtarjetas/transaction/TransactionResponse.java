package com.gentleia.landingtarjetas.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gentleia.landingtarjetas.category.CategoryResponse;
import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.TransactionType;

public record TransactionResponse(
        Long id,
        Long statementId,
        LocalDate paymentMonth,
        CardBrand cardBrand,
        CategoryResponse category,
        LocalDate transactionDate,
        String description,
        TransactionType type,
        BigDecimal amountPesos,
        BigDecimal amountUsd,
        Integer currentInstallment,
        Integer totalInstallments,
        String notes
) {
    public static TransactionResponse from(StatementTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getStatement().getId(),
                transaction.getStatement().getPaymentMonth(),
                transaction.getStatement().getCardBrand(),
                transaction.getCategory() == null ? null : CategoryResponse.from(transaction.getCategory()),
                transaction.getTransactionDate(),
                transaction.getDescription(),
                transaction.getType(),
                transaction.getAmountPesos(),
                transaction.getAmountUsd(),
                transaction.getCurrentInstallment(),
                transaction.getTotalInstallments(),
                transaction.getNotes()
        );
    }
}
