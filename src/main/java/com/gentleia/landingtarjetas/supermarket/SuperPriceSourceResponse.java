package com.gentleia.landingtarjetas.supermarket;

import java.time.Instant;

public record SuperPriceSourceResponse(
        Long id,
        String name,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static SuperPriceSourceResponse from(SuperPriceSource source) {
        return new SuperPriceSourceResponse(
                source.getId(),
                source.getName(),
                source.isActive(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }
}
