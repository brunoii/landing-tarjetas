package com.gentleia.landingtarjetas.supermarket;

import java.time.Instant;

public record SuperCategoryResponse(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        boolean active
) {
    public static SuperCategoryResponse from(SuperCategory category) {
        return new SuperCategoryResponse(
                category.getId(),
                category.getName(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.isActive()
        );
    }
}
