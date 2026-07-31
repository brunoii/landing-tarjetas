package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.util.List;

public record TicketOcrEngineResult(
        BigDecimal ocrConfidence,
        List<TicketOcrDateCandidateResponse> dateCandidates,
        List<TicketOcrSourceCandidateResponse> sourceCandidates,
        List<TicketOcrLineCandidateResponse> lineCandidates,
        List<TicketOcrDebugLineResponse> debugLines,
        List<String> warnings
) {

    public TicketOcrEngineResult(
            BigDecimal ocrConfidence,
            List<TicketOcrDateCandidateResponse> dateCandidates,
            List<TicketOcrSourceCandidateResponse> sourceCandidates,
            List<TicketOcrLineCandidateResponse> lineCandidates,
            List<String> warnings
    ) {
        this(ocrConfidence, dateCandidates, sourceCandidates, lineCandidates, List.of(), warnings);
    }
}
