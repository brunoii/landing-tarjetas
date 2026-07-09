package com.gentleia.landingtarjetas.income;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record IncomeRequest(
        @NotBlank(message = "es obligatoria") @Size(max = 240, message = "no puede superar 240 caracteres") String description,
        @NotNull(message = "es obligatorio") IncomeType incomeType,
        @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que cero") @Digits(integer = 17, fraction = 2, message = "debe tener hasta 17 dígitos enteros y 2 decimales") BigDecimal amountPesos,
        @NotBlank(message = "es obligatorio") String startMonth,
        String endMonth,
        Boolean recurringMonthly,
        Long parentIncomeId,
        @Size(max = 500, message = "no puede superar 500 caracteres") String notes
) {
}
