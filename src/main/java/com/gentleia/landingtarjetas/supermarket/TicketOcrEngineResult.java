package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.util.List;

public record TicketOcrEngineResult(
        BigDecimal ocrConfidence,
        List<TicketOcrDateCandidateResponse> dateCandidates,
        List<TicketOcrSourceCandidateResponse> sourceCandidates,
        List<TicketOcrLineCandidateResponse> lineCandidates,
        List<String> warnings
) {
}
