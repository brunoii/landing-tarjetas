package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketOcrLineCandidateResponse(
        String rawText,
        String descriptionCandidate,
        BigDecimal pricePesos,
        BigDecimal confidence,
        List<String> warnings,
        String barcodeOrStoreCode,
        BigDecimal quantity,
        BigDecimal unitPricePesos,
        BigDecimal lineTotalPesos,
        BigDecimal taxPesos,
        Long productCandidateId,
        String productCandidateName
) {

    public TicketOcrLineCandidateResponse(
            String rawText,
            String descriptionCandidate,
            BigDecimal pricePesos,
            BigDecimal confidence,
            List<String> warnings,
            Long productCandidateId,
            String productCandidateName
    ) {
        this(rawText, descriptionCandidate, pricePesos, confidence, warnings, null, null, null, null, null,
                productCandidateId, productCandidateName);
    }
}
