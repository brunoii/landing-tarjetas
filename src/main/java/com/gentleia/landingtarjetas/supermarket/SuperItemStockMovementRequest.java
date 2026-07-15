package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SuperItemStockMovementRequest(
        @NotNull(message = "es obligatoria")
        @Digits(integer = 7, fraction = 3, message = "debe tener hasta 7 enteros y 3 decimales")
        @DecimalMin(value = "0.0", inclusive = false, message = "debe ser mayor a 0") BigDecimal quantity,
        @Size(max = 500, message = "no puede superar {max} caracteres") String notes,
        Boolean allowNegativeStock
) {
    public boolean allowsNegativeStock() {
        return Boolean.TRUE.equals(allowNegativeStock);
    }
}
