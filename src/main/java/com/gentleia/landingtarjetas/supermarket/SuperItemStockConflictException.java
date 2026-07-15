package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;

public class SuperItemStockConflictException extends RuntimeException {

    private final Long itemId;
    private final String itemName;
    private final BigDecimal currentStock;
    private final BigDecimal quantity;
    private final BigDecimal resultingStock;
    private final SuperItemStockMovement.MovementType movementType;
    private final boolean allowNegativeStock;

    public SuperItemStockConflictException(SuperItem item, BigDecimal quantity, BigDecimal resultingStock,
            SuperItemStockMovement.MovementType movementType, boolean allowNegativeStock) {
        super("El consumo dejaría stock negativo. Confirme para continuar.");
        this.itemId = item.getId();
        this.itemName = item.getName();
        this.currentStock = item.getCurrentStock();
        this.quantity = quantity;
        this.resultingStock = resultingStock;
        this.movementType = movementType;
        this.allowNegativeStock = allowNegativeStock;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getResultingStock() {
        return resultingStock;
    }

    public SuperItemStockMovement.MovementType getMovementType() {
        return movementType;
    }

    public boolean isAllowNegativeStock() {
        return allowNegativeStock;
    }
}
