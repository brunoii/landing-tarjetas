package com.gentleia.landingtarjetas.statement;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.Provider;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record StatementUpdateRequest(
        @NotNull Provider provider,
        @NotNull CardBrand cardBrand,
        @Size(max = 80) String cardAlias,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate closingDate,
        LocalDate dueDate,
        LocalDate paymentMonth,
        @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal totalPesos,
        @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal totalUsd
) {
}
