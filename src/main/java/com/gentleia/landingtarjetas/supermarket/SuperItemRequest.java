package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SuperItemRequest(
        @NotBlank(message = "es obligatorio") @Size(max = SupermarketLimits.ITEM_NAME_MAX_LENGTH, message = "no puede superar {max} caracteres") String name,
        @NotNull(message = "es obligatoria") Long categoryId,
        Boolean checked,
        @Size(max = SupermarketLimits.ITEM_NOTES_MAX_LENGTH, message = "no puede superar {max} caracteres") String notes,
        @Size(max = SupermarketLimits.ITEM_UNIT_MAX_LENGTH, message = "no puede superar {max} caracteres") String unit,
        @Digits(integer = 7, fraction = 3, message = "debe tener hasta 7 enteros y 3 decimales")
        @DecimalMin(value = "0.0", inclusive = false, message = "debe ser mayor a 0") BigDecimal habitualObjective,
        @Digits(integer = 7, fraction = 3, message = "debe tener hasta 7 enteros y 3 decimales")
        @DecimalMin(value = "0.0", inclusive = false, message = "debe ser mayor a 0") BigDecimal quickQuantity,
        BigDecimal currentStock,
        @Size(max = SupermarketLimits.ITEM_PRESENTATION_LABEL_MAX_LENGTH, message = "no puede superar {max} caracteres") String commercialPresentationLabel,
        @Digits(integer = 7, fraction = 3, message = "debe tener hasta 7 enteros y 3 decimales")
        @DecimalMin(value = "0.0", inclusive = false, message = "debe ser mayor a 0") BigDecimal commercialPresentationQuantity
) {
}
