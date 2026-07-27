package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.util.List;

public record TicketOcrSourceCandidateResponse(
        String label,
        BigDecimal confidence,
        List<String> warnings
) {
}
