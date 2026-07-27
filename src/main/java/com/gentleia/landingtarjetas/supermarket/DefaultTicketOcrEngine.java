package com.gentleia.landingtarjetas.supermarket;

import java.awt.image.BufferedImage;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
class DefaultTicketOcrEngine implements TicketOcrEngine {

    @Override
    public TicketOcrEngineResult extractCandidates(BufferedImage image) {
        return new TicketOcrEngineResult(
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of("Ticket OCR engine is not configured yet")
        );
    }
}
