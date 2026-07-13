package com.gentleia.landingtarjetas.supermarket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SuperItemRequest(
        @NotBlank(message = "es obligatorio") @Size(max = SupermarketLimits.ITEM_NAME_MAX_LENGTH, message = "no puede superar {max} caracteres") String name,
        @NotNull(message = "es obligatoria") Long categoryId,
        Boolean checked,
        @Size(max = SupermarketLimits.ITEM_NOTES_MAX_LENGTH, message = "no puede superar {max} caracteres") String notes
) {
}
