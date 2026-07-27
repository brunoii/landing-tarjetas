package com.gentleia.landingtarjetas.supermarket;

import java.awt.image.BufferedImage;
import java.util.List;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class Tess4jTicketOcrEngine implements TicketOcrEngine {

    private static final String SAFE_RUNTIME_WARNING = "Ticket OCR runtime is unavailable or misconfigured; review image manually";

    private final ITesseract tesseract;
    private final TicketOcrCandidateParser parser;

    @Autowired
    public Tess4jTicketOcrEngine(TicketOcrCandidateParser parser) {
        this(configuredTesseract(), parser);
    }

    public Tess4jTicketOcrEngine(ITesseract tesseract, TicketOcrCandidateParser parser) {
        this.tesseract = tesseract;
        this.parser = parser;
    }

    @Override
    public TicketOcrEngineResult extractCandidates(BufferedImage image) {
        try {
            String rawText = tesseract.doOCR(image);
            return parser.parse(rawText, null);
        } catch (TesseractException | UnsatisfiedLinkError | RuntimeException exception) {
            return safeFailure();
        }
    }

    private static ITesseract configuredTesseract() {
        Tesseract instance = new Tesseract();
        instance.setLanguage("spa+eng");
        return instance;
    }

    private TicketOcrEngineResult safeFailure() {
        return new TicketOcrEngineResult(null, List.of(), List.of(), List.of(), List.of(SAFE_RUNTIME_WARNING));
    }
}
