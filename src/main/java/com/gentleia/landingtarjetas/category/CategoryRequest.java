package com.gentleia.landingtarjetas.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "es obligatorio") @Size(max = 80, message = "no puede superar 80 caracteres") String name,
        @Size(max = 7, message = "no puede superar 7 caracteres")
        @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "debe estar vacío o ser un color hexadecimal como #38bdf8") String color,
        Boolean active
) {
}
