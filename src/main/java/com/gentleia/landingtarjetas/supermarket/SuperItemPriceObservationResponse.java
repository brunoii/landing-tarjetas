package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SuperItemPriceObservationResponse(
        Long id,
        Long itemId,
        String itemName,
        BigDecimal pricePesos,
        Long priceSourceId,
        String sourceLabel,
        LocalDate observedDate,
        String presentationLabelSnapshot,
        BigDecimal presentationQuantitySnapshot,
        Instant createdAt
) {
    public static SuperItemPriceObservationResponse from(SuperItemPriceObservation observation) {
        return new SuperItemPriceObservationResponse(
                observation.getId(),
                observation.getItem().getId(),
                observation.getItem().getName(),
                observation.getPricePesos(),
                observation.getPriceSource() == null ? null : observation.getPriceSource().getId(),
                observation.getSourceLabel(),
                observation.getObservedDate(),
                observation.getPresentationLabelSnapshot(),
                observation.getPresentationQuantitySnapshot(),
                observation.getCreatedAt()
        );
    }
}
