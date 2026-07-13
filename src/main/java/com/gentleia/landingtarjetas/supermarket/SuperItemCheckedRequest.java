package com.gentleia.landingtarjetas.supermarket;

import jakarta.validation.constraints.NotNull;

public record SuperItemCheckedRequest(
        @NotNull(message = "es obligatorio") Boolean checked
) {
}
