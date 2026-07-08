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
        @NotNull(message = "es obligatorio") Provider provider,
        @NotNull(message = "es obligatoria") CardBrand cardBrand,
        @Size(max = 80, message = "no puede superar 80 caracteres") String cardAlias,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate closingDate,
        LocalDate dueDate,
        LocalDate paymentMonth,
        @PositiveOrZero(message = "debe ser cero o mayor") @Digits(integer = 17, fraction = 2, message = "debe tener hasta 17 dígitos enteros y 2 decimales") BigDecimal totalPesos,
        @PositiveOrZero(message = "debe ser cero o mayor") @Digits(integer = 17, fraction = 2, message = "debe tener hasta 17 dígitos enteros y 2 decimales") BigDecimal totalUsd,
        @PositiveOrZero(message = "debe ser cero o mayor") @Digits(integer = 17, fraction = 2, message = "debe tener hasta 17 dígitos enteros y 2 decimales") BigDecimal minimumPaymentPesos
) {
}
