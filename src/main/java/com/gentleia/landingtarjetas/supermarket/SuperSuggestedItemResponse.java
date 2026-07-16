package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;

public record SuperSuggestedItemResponse(
        Long itemId,
        String name,
        Long categoryId,
        String categoryName,
        String unit,
        BigDecimal habitualObjective,
        BigDecimal currentStock,
        BigDecimal suggestedQuantity
) {
    public static SuperSuggestedItemResponse from(SuperItem item) {
        return new SuperSuggestedItemResponse(
                item.getId(),
                item.getName(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.getUnit(),
                item.getHabitualObjective(),
                item.getCurrentStock(),
                item.getHabitualObjective().subtract(item.getCurrentStock())
        );
    }
}
