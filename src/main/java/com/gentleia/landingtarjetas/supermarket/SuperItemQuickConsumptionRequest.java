package com.gentleia.landingtarjetas.supermarket;

public record SuperItemQuickConsumptionRequest(
        Boolean allowNegativeStock
) {
    public boolean allowsNegativeStock() {
        return Boolean.TRUE.equals(allowNegativeStock);
    }
}
