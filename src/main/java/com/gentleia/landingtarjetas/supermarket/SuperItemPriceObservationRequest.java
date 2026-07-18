package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record SuperItemPriceObservationRequest(
        @NotNull(message = "es obligatorio")
        @Digits(integer = 10, fraction = 2, message = "debe tener hasta 10 enteros y 2 decimales")
        @DecimalMin(value = "0.0", inclusive = false, message = "debe ser mayor a 0") BigDecimal pricePesos,
        String sourceLabel,
        LocalDate observedDate
) {
}
