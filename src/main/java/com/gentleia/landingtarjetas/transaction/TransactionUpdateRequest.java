package com.gentleia.landingtarjetas.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gentleia.landingtarjetas.shared.TransactionType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TransactionUpdateRequest(
        LocalDate transactionDate,
        @NotBlank @Size(max = 240) String description,
        @NotNull TransactionType type,
        Long categoryId,
        @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal amountPesos,
        @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal amountUsd,
        @Positive Integer currentInstallment,
        @Positive Integer totalInstallments,
        @Size(max = 500) String notes
) {
}
