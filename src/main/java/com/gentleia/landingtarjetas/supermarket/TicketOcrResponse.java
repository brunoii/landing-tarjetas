package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.util.List;

public record TicketOcrResponse(
        String checksumSha256,
        String originalFilename,
        String contentType,
        long sizeBytes,
        BigDecimal ocrConfidence,
        List<TicketOcrDateCandidateResponse> dateCandidates,
        List<TicketOcrSourceCandidateResponse> sourceCandidates,
        List<TicketOcrLineCandidateResponse> lineCandidates,
        List<String> warnings
) {
}
