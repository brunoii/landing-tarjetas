package com.gentleia.landingtarjetas.supermarket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuperCategoryRequest(
        @NotBlank(message = "es obligatorio") @Size(max = SupermarketLimits.CATEGORY_NAME_MAX_LENGTH, message = "no puede superar {max} caracteres") String name,
        Boolean active
) {
}
