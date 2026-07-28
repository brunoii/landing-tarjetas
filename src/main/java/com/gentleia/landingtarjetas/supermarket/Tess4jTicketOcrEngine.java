package com.gentleia.landingtarjetas.supermarket;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class Tess4jTicketOcrEngine implements TicketOcrEngine {

    private static final String DATAPATH_WARNING = "ticket-ocr-runtime-datapath: Ticket OCR datapath is not configured; review image manually";
    private static final String NATIVE_WARNING = "ticket-ocr-runtime-native: Ticket OCR native runtime is unavailable; review image manually";
    private static final String LANGDATA_WARNING = "ticket-ocr-runtime-langdata: Ticket OCR language data is unavailable; review image manually";
    private static final String SAFE_RUNTIME_WARNING = "ticket-ocr-runtime-misconfigured: Ticket OCR runtime is unavailable or misconfigured; review image manually";

    private final ITesseract tesseract;
    private final TicketOcrCandidateParser parser;
    private final TicketOcrUploadProperties properties;

    @Autowired
    public Tess4jTicketOcrEngine(TicketOcrUploadProperties properties, TicketOcrCandidateParser parser) {
        this(configuredTesseract(properties), parser, properties);
    }

    public Tess4jTicketOcrEngine(ITesseract tesseract, TicketOcrCandidateParser parser) {
        this(tesseract, parser, defaultProperties());
    }

    public Tess4jTicketOcrEngine(ITesseract tesseract, TicketOcrCandidateParser parser, TicketOcrUploadProperties properties) {
        this.tesseract = tesseract;
        this.parser = parser;
        this.properties = properties;
    }

    @Override
    public TicketOcrEngineResult extractCandidates(BufferedImage image) {
        if (isBlank(properties.getDatapath())) {
            return safeFailure(DATAPATH_WARNING);
        }
        try {
            String rawText = tesseract.doOCR(image);
            return parser.parse(rawText, null);
        } catch (UnsatisfiedLinkError exception) {
            return safeFailure(NATIVE_WARNING);
        } catch (TesseractException exception) {
            return safeFailure(classifyTesseractFailure(exception));
        } catch (RuntimeException exception) {
            return safeFailure(SAFE_RUNTIME_WARNING);
        }
    }

    private static ITesseract configuredTesseract(TicketOcrUploadProperties properties) {
        Tesseract instance = new Tesseract();
        if (!isBlank(properties.getDatapath())) {
            instance.setDatapath(properties.getDatapath().trim());
        }
        instance.setLanguage(normalizedLanguages(properties));
        return instance;
    }

    private static TicketOcrUploadProperties defaultProperties() {
        TicketOcrUploadProperties properties = new TicketOcrUploadProperties();
        properties.setDatapath("configured-for-tests");
        return properties;
    }

    private static String normalizedLanguages(TicketOcrUploadProperties properties) {
        return isBlank(properties.getLanguages()) ? "spa+eng" : properties.getLanguages().trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String classifyTesseractFailure(TesseractException exception) {
        String message = exception.getMessage();
        if (message != null) {
            String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("traineddata") || normalized.contains("error opening data file") || normalized.contains("failed loading language")) {
                return LANGDATA_WARNING;
            }
        }
        return SAFE_RUNTIME_WARNING;
    }

    private TicketOcrEngineResult safeFailure(String warning) {
        return new TicketOcrEngineResult(null, List.of(), List.of(), List.of(), List.of(warning));
    }
}
