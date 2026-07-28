package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.gentleia.landingtarjetas.supermarket.Tess4jTicketOcrEngine;
import com.gentleia.landingtarjetas.supermarket.TicketOcrCandidateParser;
import com.gentleia.landingtarjetas.supermarket.TicketOcrEngineResult;
import com.gentleia.landingtarjetas.supermarket.TicketOcrUploadProperties;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;

import org.junit.jupiter.api.Test;

class Tess4jTicketOcrEngineTests {

    @Test
    void returnsSanitizedDatapathWarningWhenConfigurationIsMissing() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ITesseract tesseract = mock(ITesseract.class);
        Tess4jTicketOcrEngine engine = new Tess4jTicketOcrEngine(
                tesseract,
                new TicketOcrCandidateParser(),
                properties("", "spa+eng"));

        TicketOcrEngineResult result = engine.extractCandidates(image);

        verifyNoCandidates(result);
        assertThat(result.warnings()).containsExactly("ticket-ocr-runtime-datapath: Ticket OCR datapath is not configured; review image manually");
    }

    @Test
    void returnsSanitizedNativeRuntimeWarningWithoutLeakingErrorDetails() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ITesseract tesseract = mock(ITesseract.class);
        when(tesseract.doOCR(same(image))).thenThrow(new UnsatisfiedLinkError("Missing native runtime at C:/secret/native"));
        Tess4jTicketOcrEngine engine = new Tess4jTicketOcrEngine(
                tesseract,
                new TicketOcrCandidateParser(),
                properties("C:/configured/tessdata", "spa+eng"));

        TicketOcrEngineResult result = engine.extractCandidates(image);

        verifyNoCandidates(result);
        assertThat(result.warnings()).containsExactly("ticket-ocr-runtime-native: Ticket OCR native runtime is unavailable; review image manually");
    }

    @Test
    void returnsSanitizedLanguageDataWarningWithoutLeakingPaths() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ITesseract tesseract = mock(ITesseract.class);
        when(tesseract.doOCR(same(image))).thenThrow(new TesseractException("Error opening data file C:/secret/tessdata/spa.traineddata"));
        Tess4jTicketOcrEngine engine = new Tess4jTicketOcrEngine(
                tesseract,
                new TicketOcrCandidateParser(),
                properties("C:/configured/tessdata", "spa+eng"));

        TicketOcrEngineResult result = engine.extractCandidates(image);

        verifyNoCandidates(result);
        assertThat(result.warnings()).containsExactly("ticket-ocr-runtime-langdata: Ticket OCR language data is unavailable; review image manually");
        assertThat(result.warnings().get(0)).doesNotContain("C:/secret", "spa.traineddata");
    }

    @Test
    void usesBufferedImageInputAndParsesTransientTextWithoutTempArtifacts() throws Exception {
        Path tempDirectory = Files.createTempDirectory("ticket-ocr-engine-test");
        Set<String> before = listNames(tempDirectory);
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ITesseract tesseract = mock(ITesseract.class);
        when(tesseract.doOCR(same(image))).thenReturn("Supermercado Central\nFecha 21/07/2026\nYERBA 1KG 2500,50");
        Tess4jTicketOcrEngine engine = new Tess4jTicketOcrEngine(tesseract, new TicketOcrCandidateParser());

        TicketOcrEngineResult result = engine.extractCandidates(image);

        verify(tesseract).doOCR(same(image));
        assertThat(result.sourceCandidates()).singleElement()
                .satisfies(candidate -> assertThat(candidate.label()).isEqualTo("Supermercado Central"));
        assertThat(result.lineCandidates()).singleElement()
                .satisfies(candidate -> assertThat(candidate.pricePesos()).isEqualByComparingTo("2500.50"));
        assertThat(listNames(tempDirectory)).isEqualTo(before);
        Files.delete(tempDirectory);
    }

    @Test
    void returnsSafeWarningWhenRuntimeIsUnavailableOrMisconfigured() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ITesseract tesseract = mock(ITesseract.class);
        when(tesseract.doOCR(same(image))).thenThrow(new TesseractException("tessdata path C:/secret/ticket.png is invalid"));
        Tess4jTicketOcrEngine engine = new Tess4jTicketOcrEngine(
                tesseract,
                new TicketOcrCandidateParser());

        TicketOcrEngineResult result = engine.extractCandidates(image);

        verifyNoCandidates(result);
        assertThat(result.warnings()).containsExactly("ticket-ocr-runtime-misconfigured: Ticket OCR runtime is unavailable or misconfigured; review image manually");
    }

    private TicketOcrUploadProperties properties(String datapath, String languages) {
        TicketOcrUploadProperties properties = new TicketOcrUploadProperties();
        properties.setDatapath(datapath);
        properties.setLanguages(languages);
        return properties;
    }

    private void verifyNoCandidates(TicketOcrEngineResult result) {
        assertThat(result.lineCandidates()).isEmpty();
        assertThat(result.dateCandidates()).isEmpty();
        assertThat(result.sourceCandidates()).isEmpty();
    }

    private Set<String> listNames(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            return stream.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet());
        }
    }

}
