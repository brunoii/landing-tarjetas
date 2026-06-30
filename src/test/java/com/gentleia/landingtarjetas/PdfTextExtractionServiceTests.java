package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import com.gentleia.landingtarjetas.statement.PdfTextExtractionService;
import com.gentleia.landingtarjetas.statement.StatementUploadProperties;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PdfTextExtractionServiceTests {

    @Test
    void validationRejectsEmptyPdf() {
        PdfTextExtractionService service = serviceWithLimit(1024);
        MockMultipartFile file = new MockMultipartFile("files", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void validationRejectsNonPdfFile() {
        PdfTextExtractionService service = serviceWithLimit(1024);
        MockMultipartFile file = new MockMultipartFile("files", "fixture.txt", "text/plain",
                "synthetic text".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PDF files");
    }

    @Test
    void validationRejectsOversizedPdfBeforeExtraction() {
        PdfTextExtractionService service = serviceWithLimit(4);
        MockMultipartFile file = new MockMultipartFile("files", "oversized.pdf", "application/pdf",
                "not a real pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size limit");
    }

    @Test
    void hashUsesSha256ForLocalSourceTracking() {
        PdfTextExtractionService service = serviceWithLimit(1024);

        String hash = service.sha256("synthetic bytes".getBytes(StandardCharsets.UTF_8));

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    private PdfTextExtractionService serviceWithLimit(long bytes) {
        StatementUploadProperties properties = new StatementUploadProperties();
        properties.setMaxFileSizeBytes(bytes);
        return new PdfTextExtractionService(properties);
    }
}
