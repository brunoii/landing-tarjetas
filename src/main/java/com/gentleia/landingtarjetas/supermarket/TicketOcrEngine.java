package com.gentleia.landingtarjetas.supermarket;

import java.awt.image.BufferedImage;

public interface TicketOcrEngine {

    TicketOcrEngineResult extractCandidates(BufferedImage image);
}
