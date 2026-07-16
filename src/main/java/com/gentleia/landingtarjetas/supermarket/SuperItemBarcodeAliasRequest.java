package com.gentleia.landingtarjetas.supermarket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuperItemBarcodeAliasRequest(
        @NotBlank(message = "es obligatorio") @Size(max = SupermarketLimits.BARCODE_CODE_MAX_LENGTH, message = "no puede superar {max} caracteres") String code,
        @Size(max = SupermarketLimits.BARCODE_FORMAT_MAX_LENGTH, message = "no puede superar {max} caracteres") String format
) {
}
