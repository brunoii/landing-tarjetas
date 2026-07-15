package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.time.Instant;

public record SuperItemStockMovementResponse(
        Long id,
        Long itemId,
        String itemName,
        String itemUnit,
        SuperItemStockMovement.MovementType movementType,
        BigDecimal quantity,
        BigDecimal previousStock,
        BigDecimal resultingStock,
        String notes,
        String source,
        Instant createdAt
) {
    public static SuperItemStockMovementResponse from(SuperItemStockMovement movement) {
        return new SuperItemStockMovementResponse(
                movement.getId(),
                movement.getItem().getId(),
                movement.getItem().getName(),
                movement.getItem().getUnit(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getPreviousStock(),
                movement.getResultingStock(),
                movement.getNotes(),
                movement.getSource(),
                movement.getCreatedAt()
        );
    }
}
