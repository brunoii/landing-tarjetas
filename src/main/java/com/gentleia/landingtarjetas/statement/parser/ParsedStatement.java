package com.gentleia.landingtarjetas.statement.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.Provider;

public record ParsedStatement(
        Provider provider,
        CardBrand cardBrand,
        String cardAlias,
        LocalDate closingDate,
        LocalDate dueDate,
        LocalDate paymentMonth,
        BigDecimal totalPesos,
        BigDecimal totalUsd,
        BigDecimal minimumPaymentPesos,
        List<ParsedTransaction> transactions,
        List<String> warnings
) {
}
