package com.gentleia.landingtarjetas.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 7) @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "Category color must be empty or a hex color like #38bdf8") String color,
        Boolean active
) {
}
