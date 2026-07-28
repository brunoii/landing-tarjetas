package com.gentleia.landingtarjetas.shared;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        List<String> details,
        String outcome,
        Long itemId,
        String itemName,
        Object currentStock,
        Object quantity,
        Object resultingStock,
        String movementType,
        Boolean allowNegativeStock
) {

    public static ApiErrorResponse of(int status, String error, List<String> details) {
        return new ApiErrorResponse(Instant.now(), status, error, details, null, null, null, null, null, null, null, null);
    }

    public static ApiErrorResponse of(int status, String error, List<String> details, String outcome) {
        return new ApiErrorResponse(Instant.now(), status, error, details, outcome, null, null, null, null, null, null, null);
    }

    public static ApiErrorResponse stockConflict(int status, String error, List<String> details, Long itemId, String itemName,
            Object currentStock, Object quantity, Object resultingStock, String movementType, Boolean allowNegativeStock) {
        return new ApiErrorResponse(Instant.now(), status, error, details, null, itemId, itemName, currentStock, quantity, resultingStock,
                movementType, allowNegativeStock);
    }
}
