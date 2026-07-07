package com.gentleia.landingtarjetas.statement.parser;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gentleia.landingtarjetas.shared.TransactionType;

public record ParsedTransaction(
        LocalDate transactionDate,
        String description,
        TransactionType type,
        BigDecimal amountPesos,
        BigDecimal amountUsd,
        Integer currentInstallment,
        Integer totalInstallments,
        String operationNumber,
        String notes
) {
}
