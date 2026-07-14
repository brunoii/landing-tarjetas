package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.time.Instant;

public record SuperItemResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        boolean checked,
        String notes,
        String unit,
        BigDecimal habitualObjective,
        boolean configured,
        Instant createdAt,
        Instant updatedAt,
        boolean active
) {
    public static SuperItemResponse from(SuperItem item) {
        return new SuperItemResponse(
                item.getId(),
                item.getName(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.isChecked(),
                item.getNotes(),
                item.getUnit(),
                item.getHabitualObjective(),
                item.isConfigured(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.isActive()
        );
    }
}
