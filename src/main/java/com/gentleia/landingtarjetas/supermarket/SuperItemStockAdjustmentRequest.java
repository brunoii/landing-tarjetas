package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record SuperItemStockAdjustmentRequest(
        @NotNull(message = "es obligatorio")
        @Digits(integer = 7, fraction = 3, message = "debe tener hasta 7 enteros y 3 decimales")
        @DecimalMin(value = "0.0", inclusive = true, message = "debe ser mayor o igual a 0") BigDecimal currentStock
) {
}
