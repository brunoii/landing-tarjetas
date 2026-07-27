package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TicketOcrDateCandidateResponse(
        LocalDate value,
        BigDecimal confidence,
        List<String> warnings
) {
}
