package com.gentleia.landingtarjetas.supermarket;

import java.time.Instant;

public record SuperItemBarcodeAliasResponse(
        Long id,
        Long itemId,
        String code,
        String format,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Instant deactivatedAt
) {
    public static SuperItemBarcodeAliasResponse from(SuperItemBarcodeAlias alias) {
        return new SuperItemBarcodeAliasResponse(
                alias.getId(),
                alias.getItem().getId(),
                alias.getCode(),
                alias.getFormat(),
                alias.isActive(),
                alias.getCreatedAt(),
                alias.getUpdatedAt(),
                alias.getDeactivatedAt()
        );
    }
}
